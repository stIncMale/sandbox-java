package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import static stinc.male.sandbox.ratexecutor.Preconditions.checkNotNull;

/**
 * This implementation of {@link RateMeter} is not linearizable and may produce inaccurate results if used concurrently
 * (see {@link RateMeter} for details).
 * @param <T>
 */
public abstract class AbstractNavigableMapRateMeter<T extends NavigableMap<Long, TicksCounter>> extends AbstractRateMeter {
  private static final int MAX_OPTIMISTIC_READ_ATTEMPTS = 3;

  private final boolean sequential;
  private final T samples;
  private final long timeSensitivityNanos;
  private final AtomicBoolean gcInProgress;
  private volatile long gcLastRightSamplesWindowBoundary;
  @Nullable
  private final LongAdder failedAccuracyEventsCount;//null if singleThreaded == true

  /**
   * (0,Double.MAX_VALUE]<br>
   * The bigger, the less frequently GC happens, but the more elements are maintained in samples.
   */
  private final double gcFactor = 0.3;

  /**
   * @param startNanos Starting point that is used to calculate elapsed nanoseconds.
   * @param samplesInterval Size of the samples window.
   * @param config Additional configuration parameters.
   * @param samplesSuppplier Specifies a supplier to use to create an object that will be
   *                         used to store samples and will be returned by {@link #getSamples()}.
   *                         The {@link NavigableMap} provided by this supplier must use {@link NanosComparator}
   *                         as {@link NavigableMap#comparator() comparator}.
   * @param sequential
   */
  public AbstractNavigableMapRateMeter(
      final long startNanos,
      final Duration samplesInterval,
      final RateMeterConfig config,
      final Supplier<T> samplesSuppplier,
      final boolean sequential) {
    super(startNanos, samplesInterval, config);
    checkNotNull(samplesSuppplier, "samplesSuppplier");
    samples = samplesSuppplier.get();
    Preconditions.checkArgument(samples.comparator() instanceof NanosComparator, "samplesSupplier", "The comparator used by samples map must be of type NanosComparator");
    samples.put(startNanos, config.getTicksCounterSupplier().apply(0L));
    gcInProgress = new AtomicBoolean();
    gcLastRightSamplesWindowBoundary = getStartNanos();
    timeSensitivityNanos = config.getTimeSensitivity().toNanos();
    Preconditions.checkArgument(timeSensitivityNanos <= getSamplesIntervalNanos(), "config",
        () -> String.format("timeSensitivityNanos = %s must be not greater than getSamplesIntervalNanos() = %s",
            timeSensitivityNanos, getSamplesIntervalNanos()));
    this.sequential = sequential;
    failedAccuracyEventsCount = sequential ? null : new LongAdder();
  }

  @Override
  public long rightSamplesWindowBoundary() {
    return samples.lastKey();
  }

  @Override
  public long ticksCount() {
    long result = 0;
    final long samplesIntervalNanos = getSamplesIntervalNanos();
    long rightNanos = rightSamplesWindowBoundary();
    if (sequential) {
      final long leftNanos = rightNanos - samplesIntervalNanos;
      result = count(leftNanos, rightNanos);
    } else {
      for (int ri = 0; ri < MAX_OPTIMISTIC_READ_ATTEMPTS; ri++) {
        final long leftNanos = rightNanos - samplesIntervalNanos;
        result = count(leftNanos, rightNanos);
        final long newRightNanos = rightSamplesWindowBoundary();
        if (NanosComparator.compare(newRightNanos - 2 * samplesIntervalNanos, leftNanos) <= 0) {//the samples window may has been moved while we were counting, but result is still correct
          break;
        } else {//the samples window has been moved too far
          rightNanos = newRightNanos;
          if (ri == MAX_OPTIMISTIC_READ_ATTEMPTS - 1) {//all read attempts have been exhausted, return what we have
            failedAccuracyEventsCount.increment();
          }
        }
      }
    }
    return result;
  }

  @Override
  public void tick(final long count, final long tNanos) {
    checkArgument(tNanos, "tNanos");
    if (count != 0) {
      final long rightNanos = rightSamplesWindowBoundary();
      final long leftNanos = rightNanos - getSamplesIntervalNanos();
      if (NanosComparator.compare(leftNanos, tNanos) < 0) {//tNanos is within or ahead of the samples window
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
    long rightNanos = rightSamplesWindowBoundary();
    final long leftNanos = rightNanos - samplesIntervalNanos;
    final long count;
    final long effectiveTNanos;
    if (NanosComparator.compare(tNanos, leftNanos) <= 0) {//tNanos is behind the samples window, so return average over all samples
      count = ticksTotalCount();
      effectiveTNanos = rightNanos;
    } else if (NanosComparator.compare(rightNanos, tNanos) <= 0) {//tNanos is ahead of the samples window or exactly on the right border
      count = ticksTotalCount();
      effectiveTNanos = tNanos;
    } else {//tNanos is within the samples window and not on the right border
      final long substractCount = count(tNanos, rightNanos);
      if (sequential) {
        count = ticksTotalCount() - substractCount;
        effectiveTNanos = tNanos;
      } else {
        final long newRightNanos = rightSamplesWindowBoundary();
        if (NanosComparator.compare(newRightNanos - 2 * samplesIntervalNanos, tNanos) <= 0) {//the samples window may has been moved while we were counting, but substractCount is still correct
          count = ticksTotalCount() - substractCount;
          effectiveTNanos = tNanos;
        } else {//the samples window has been moved too far, so average over all samples is the best we can do
          failedAccuracyEventsCount.increment();
          count = ticksTotalCount();
          effectiveTNanos = newRightNanos;
        }
      }
    }
    return RateMeterMath.rateAverage(effectiveTNanos, samplesIntervalNanos, getStartNanos(), count);
  }

  @Override
  public double rate(final long tNanos) {//TODO specify that the closer tNanos to the right border, the more accurate the result
    checkArgument(tNanos, "tNanos");
    final double result;
    final long samplesIntervalNanos = getSamplesIntervalNanos();
    final long rightNanos = rightSamplesWindowBoundary();
    final long leftNanos = rightNanos - samplesIntervalNanos;
    if (NanosComparator.compare(tNanos, leftNanos) <= 0) {//tNanos is behind the samples window, so return average over all samples
      result = RateMeterMath.rateAverage(rightNanos, samplesIntervalNanos, getStartNanos(), ticksTotalCount());//this is the same as rateAverage(), or rateAverage(rightNanos) if rightNanos == rightSamplesWindowBoundary()
    } else {//tNanos is within or ahead of the samples window
      final long effectiveLeftNanos = tNanos - samplesIntervalNanos;
      if (NanosComparator.compare(rightNanos, effectiveLeftNanos) <= 0) {//tNanos is way too ahead of the samples window and the are no samples for the requested tNanos
        result = 0;
      } else {
        if (sequential) {
          result = count(effectiveLeftNanos, tNanos);
        } else {
          long count = count(effectiveLeftNanos, tNanos);
          final long newRightNanos = rightSamplesWindowBoundary();
          final long safeLeft = newRightNanos - 2 * samplesIntervalNanos;
          if (NanosComparator.compare(safeLeft, effectiveLeftNanos) <= 0) {//the samples window may has been moved while we were counting, but count is still correct
            result = count;
          } else {//the samples window has been moved too far
            failedAccuracyEventsCount.increment();
            if (NanosComparator.compare(tNanos, rightNanos) < 0 && NanosComparator.compare(safeLeft, leftNanos) <= 0) {//tNanos is within the samples window and we still have a chance to calculate rate for rightNanos
              count = count(leftNanos, rightNanos);
              final long newNewRightNanos = rightSamplesWindowBoundary();
              if (NanosComparator.compare(newNewRightNanos - 2 * samplesIntervalNanos, leftNanos) <= 0) {//the samples window may has been moved while we were counting, but count is still correct
                result = count;
              } else {//average over all samples is the best we can do
                result = RateMeterMath.rateAverage(newNewRightNanos, samplesIntervalNanos, getStartNanos(), ticksTotalCount());//this is the same as rateAverage(), or rateAverage(rightNanos) if rightNanos == rightSamplesWindowBoundary()
              }
            } else {//average over all samples is the best we can do
              result = RateMeterMath.rateAverage(newRightNanos, samplesIntervalNanos, getStartNanos(), ticksTotalCount());
            }
          }
        }
      }
    }
    return result;
  }

  public final long failedAccuracyEventsCount() {
    return failedAccuracyEventsCount.sum();
  }

  private final long count(final long fromExclusiveNanos, final long toInclusiveNanos) {
    return samples.subMap(fromExclusiveNanos, false, toInclusiveNanos, true)
        .values()
        .stream()
        .mapToLong(TicksCounter::get)
        .sum();
  }

  private final boolean gcRequired(final long rightSamplesWindowBoundary) {
    final long samplesWindowShiftNanos = rightSamplesWindowBoundary - gcLastRightSamplesWindowBoundary;
    final long samplesIntervalNanos = getSamplesIntervalNanos();
    final boolean result = (double)samplesWindowShiftNanos / samplesIntervalNanos >= 2 + gcFactor;//2 is required to maintain history span of 2 * samplesIntervalNanos
    return result;
  }

  private final void gc(final long rightSamplesWindowBoundary) {
    if (gcInProgress.compareAndSet(false, true)) {
      try {
        gcLastRightSamplesWindowBoundary = rightSamplesWindowBoundary;
        final long leftNanos = rightSamplesWindowBoundary - 2 * getSamplesIntervalNanos();//2 is required to maintain history span 2 * samplesIntervalNanos
        @Nullable
        final Long firstNanos = samples.firstKey();
        if (firstNanos != null && NanosComparator.compare(firstNanos.longValue(), leftNanos) < 0) {
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
   * {@link #AbstractNavigableMapRateMeter(long, Duration, RateMeterConfig, Supplier, boolean)} by using the fourth argument.
   * The returned instance MUST NOT contain {@code null} values (one MUST NOT put such values inside).
   * @see #AbstractNavigableMapRateMeter(long, Duration, RateMeterConfig, Supplier, boolean)
   */
  protected T getSamples() {
    return samples;
  }
}