package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import static stinc.male.sandbox.ratexecutor.Preconditions.checkNotNull;

/**
 * This implementation of {@link RateMeter} is not linearizable and may produce inaccurate results if used concurrently
 * (see {@link RateMeter} for details).
 * @param <T>
 */
public abstract class AbstractNavigableMapRateMeter<T extends NavigableMap<Long, TicksCounter>> extends AbstractRateMeter {
  private final T samples;
  private final long timeSensitivityNanos;
  private final AtomicBoolean gcInProgress;
  private volatile long gcLastRightSamplesWindowBoundary;
  /**
   * (0,Double.MAX_VALUE]<br>
   * The bigger, the less frequently GC happens, but the more elements are maintained in samples.
   */
  private final double gcFactor = 0.3;

  /**
   * @param startNanos Starting point that is used to calculate elapsed nanoseconds.
   * @param samplesInterval Size of the samples window.
   * @param config Additional configuration parameters.
   * @param samplesSuppplier Specifies a supplier to use to create an object that will be returned by {@link #getSamples()}.
   * The {@link NavigableMap} provided by this supplier MUST use {@link NavigableMap#comparator() comparator}
   * that compares nanoseconds according to {@link System#nanoTime()} specification.
   */
  public AbstractNavigableMapRateMeter(
      final long startNanos,
      final Duration samplesInterval,
      final RateMeterConfig config,
      final Supplier<T> samplesSuppplier) {
    super(startNanos, samplesInterval, config);
    checkNotNull(samplesSuppplier, "samplesSuppplier");
    samples = samplesSuppplier.get();
    samples.put(startNanos, config.getTicksCounterSupplier().apply(0L));
    gcInProgress = new AtomicBoolean();
    gcLastRightSamplesWindowBoundary = getStartNanos();
    timeSensitivityNanos = config.getTimeSensitivity().toNanos();
    Preconditions.checkArgument(timeSensitivityNanos <= getSamplesIntervalNanos(), "config",
        () -> String.format("timeSensitivityNanos = %s must be not greater than getSamplesIntervalNanos() = %s",
            timeSensitivityNanos, getSamplesIntervalNanos()));
  }

  @Override
  public long rightSamplesWindowBoundary() {
    return samples.lastKey();
  }

  @Override
  public long ticksCount() {
    final long rightNanos = rightSamplesWindowBoundary();
    return count(rightNanos - getSamplesIntervalNanos(), rightNanos);
  }

  @Override
  public void tick(final long count, final long tNanos) {
    checkArgument(tNanos, "tNanos");
    if (count != 0) {
      final long rightNanos = rightSamplesWindowBoundary();
      final long leftNanos = rightNanos - getSamplesIntervalNanos();
      if (samples.comparator().compare(leftNanos, tNanos) < 0) {//tNanos is not behind the samples window (i.e. within or ahead)
        @Nullable
        final TicksCounter existingSample;
        if (timeSensitivityNanos == 1) {
          final TicksCounter newSample = getConfig().getTicksCounterSupplier().apply(count);
          existingSample = samples.putIfAbsent(tNanos, newSample);
        } else {
          @Nullable
          final Entry<Long, TicksCounter> existingEntry = samples.floorEntry(tNanos);
          if (existingEntry != null && (tNanos - existingEntry.getKey()) <= timeSensitivityNanos) {
            existingSample = existingEntry.getValue();
          } else {
            final TicksCounter newSample = getConfig().getTicksCounterSupplier().apply(count);
            existingSample = samples.putIfAbsent(tNanos, newSample);
          }
        }
        if (existingSample != null) {//we need to merge samples
          existingSample.add(count);
        }
      }
      getTicksTotalCounter().add(count);
      if (gcRequired(rightNanos)) {
        gc(rightNanos);
      }
    }
  }

  @Override
  public double rateAverage(final long tNanos) {
    checkArgument(tNanos, "tNanos");
    final long samplesIntervalNanos = getSamplesIntervalNanos();
    final long rightNanos = rightSamplesWindowBoundary();
    final long leftNanos = rightNanos - samplesIntervalNanos;
    final long count;
    final long effectiveTNanos;
    final Comparator<? super Long> comparator = samples.comparator();//TODO store in a separate variable, check is comparator is NanosComparator and use it instead of Comparator
    if (comparator.compare(tNanos, leftNanos) <= 0) {//tNanos is behind the samples window, so return average over all samples
      count = ticksTotalCount();
      effectiveTNanos = rightNanos;
    } else if (comparator.compare(tNanos, rightNanos) >= 0) {//tNanos is ahead the samples window or exactly on the right border
      count = ticksTotalCount();
      effectiveTNanos = tNanos;
    } else {//tNanos is within the samples window and not on the right border
      count = ticksTotalCount() - count(tNanos, rightNanos);
      effectiveTNanos = tNanos;
    }
    return RateMeterMath.rateAverage(effectiveTNanos, samplesIntervalNanos, getStartNanos(), count);
  }

  @Override
  public double rate(final long tNanos) {
    checkArgument(tNanos, "tNanos");
    final double result;
    final long samplesIntervalNanos = getSamplesIntervalNanos();
    final long rightNanos = rightSamplesWindowBoundary();
    final long leftNanos = rightNanos - samplesIntervalNanos;
    final Comparator<? super Long> comparator = samples.comparator();
    if (comparator.compare(tNanos, leftNanos) <= 0) {//tNanos is behind the samples window, so return average over all samples
      result = rateAverage();//TODO create rateAvg (internal implementation) and use it
    } else {
      final long effectiveRightNanos = maxTNanos(rightNanos, tNanos, comparator);
      final long effectiveLeftNanos = effectiveRightNanos - samplesIntervalNanos;
      if (effectiveLeftNanos >= rightNanos) {//tNanos is way too ahead the samples window and the are no samples for the requested tNanos
        result = 0;
      } else {
        result = count(effectiveLeftNanos, effectiveRightNanos);
      }
    }
    return result;
  }

  private final long count(final long fromExclusiveNanos, final long toInclusiveNanos) {//TODO check if we still inside the window, otherwise ? Count such fails
    return samples.subMap(fromExclusiveNanos, false, toInclusiveNanos, true)
        .values()
        .stream()
        .mapToLong(TicksCounter::get)
        .sum();
  }

  private final boolean gcRequired(final long rightSamplesWindowBoundary) {
    final long samplesWindowShiftNanos = rightSamplesWindowBoundary - gcLastRightSamplesWindowBoundary;
    final long samplesIntervalNanos = getSamplesIntervalNanos();
    final boolean result = (double)samplesWindowShiftNanos / samplesIntervalNanos >= 2 + gcFactor;//2 is required to maintain history of duration 2 * samplesIntervalNanos
    return result;
  }

  private final void gc(final long rightSamplesWindowBoundary) {
    if (gcInProgress.compareAndSet(false, true)) {
      try {
        gcLastRightSamplesWindowBoundary = rightSamplesWindowBoundary;
        final long leftNanos = rightSamplesWindowBoundary - 2 * getSamplesIntervalNanos();//2 is required to maintain history of duration 2 * samplesIntervalNanos
        @Nullable
        final Long firstNanos = samples.firstKey();
        if (firstNanos != null && samples.comparator().compare(firstNanos, leftNanos) < 0) {
          samples.subMap(firstNanos, true, leftNanos, false)//do not delete sample at leftNanos, because we still need it
              .clear();
        }
      } finally {
        gcInProgress.set(false);
      }
    }
  }

  /**
   * @return A {@link NavigableMap} with samples (keys are tNanos and values are corresponding ticks counters).
   * This method always return the same instance that was created in
   * {@link #AbstractNavigableMapRateMeter(long, Duration, RateMeterConfig, Supplier)} by using the fourth argument.
   * The returned instance MUST NOT contain {@code null} values (one MUST NOT put such values inside).
   * @see #AbstractNavigableMapRateMeter(long, Duration, RateMeterConfig, Supplier)
   */
  protected T getSamples() {
    return samples;
  }

  private static final long maxTNanos(final long v1, final long v2, final Comparator<? super Long> comparator) {
    return comparator.compare(v1, v2) >= 0 ? v1 : v2;
  }
}