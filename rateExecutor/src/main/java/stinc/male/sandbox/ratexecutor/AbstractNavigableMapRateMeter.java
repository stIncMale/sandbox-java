package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import java.util.Comparator;
import java.util.NavigableMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import static stinc.male.sandbox.ratexecutor.Preconditions.checkNotNull;
import static stinc.male.sandbox.ratexecutor.RateMeterMath.convertRate;

public abstract class AbstractNavigableMapRateMeter extends AbstractRateMeter {
  private final NavigableMap<Long, TicksCounter> samples;
  private final AtomicBoolean gcInProgress;
  private volatile long gcCount;
  private volatile long gcLastRightSamplesWindowBoundary;
  /**
   * The bigger, the less frequently GC happens.
   */
  private volatile double gcFactor;
  /**
   * The bigger, the less frequently {@link #gcFactor} is recalculated.
   */
  private final long gcAdjustmentFactor;
  private final int gcTargetSamplesSise;

  /**
   * @param startNanos Starting point that is used to calculate elapsed nanoseconds.
   * @param samplesInterval Size of the samples window.
   * @param config Additional configuration parameters.
   * @param samplesSuppplier Specifies a supplier to use to create an object that will be returned by {@link #getSamples()}.
   * The {@link NavigableMap} provided by this supplier MUST use {@link NavigableMap#comparator() comparator}
   * that compares nanoseconds according to {@link System#nanoTime()} specification.
   */
  AbstractNavigableMapRateMeter(
      final long startNanos,
      final Duration samplesInterval,
      final RateMeterConfig config,
      final Supplier<? extends NavigableMap<Long, TicksCounter>> samplesSuppplier) {
    super(startNanos, samplesInterval, config);
    checkNotNull(samplesSuppplier, "samplesSuppplier");
    samples = samplesSuppplier.get();
    samples.put(startNanos, getConfig().getTicksCounterSupplier().apply(0L));
    gcInProgress = new AtomicBoolean();
    gcCount = 0;
    gcLastRightSamplesWindowBoundary = getStartNanos();
    gcFactor = 10;
    gcAdjustmentFactor = 2;
    gcTargetSamplesSise = 50_000;
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
      if (samples.comparator().compare(leftNanos, tNanos) < 0) {//tNanos is within the samples window
        final TicksCounter newSample = getConfig().getTicksCounterSupplier().apply(count);
        @Nullable
        final TicksCounter existingSample = samples.putIfAbsent(tNanos, newSample);
        if (existingSample != null) {//we need to merge samples
          existingSample.add(count);
        }
      }
      getTicksTotalCounter().add(count);
      if (gcRequired(rightNanos)) {
        gc();
      }
    }
  }

  @Override
  public double rate(final Duration unit) {
    checkArgument(unit, "unit");
    return convertRate(rate(), getSamplesIntervalNanos(), unit.toNanos());
  }

  @Override
  public double rate(final long tNanos) {
    checkArgument(tNanos, "tNanos");
    final double result;
    final long rightNanos = rightSamplesWindowBoundary();
    if (samples.comparator().compare(tNanos, rightNanos) < 0) {
      result = rateAverage();
    } else {
      final long samplesIntervalNanos = getSamplesIntervalNanos();
      final long effectiveRightNanos = max(rightNanos, tNanos, samples.comparator());
      final long effectiveLeftNanos = effectiveRightNanos - samplesIntervalNanos;
      result = count(effectiveLeftNanos, effectiveRightNanos);
    }
    return result;
  }

  @Override
  public double rate(final long tNanos, final Duration unit) {
    checkArguments(tNanos, "tNanos", unit, "unit");
    return convertRate(rate(tNanos), getSamplesIntervalNanos(), unit.toNanos());
  }

  private final long count(final long fromExclusiveNanos, final long toInclusiveNanos) {
    return samples.subMap(fromExclusiveNanos, false, toInclusiveNanos, true)
        .values()
        .stream()
        .mapToLong(TicksCounter::get)
        .sum();
  }

  private final boolean gcRequired(final long rightSamplesWindowBoundary) {
    final long shift = rightSamplesWindowBoundary - gcLastRightSamplesWindowBoundary;
    final long samplesIntervalNanos = getSamplesIntervalNanos();
    final boolean result;
    double gcFactor = this.gcFactor;
    if (shift > samplesIntervalNanos) {
      result = (double)shift / samplesIntervalNanos >= gcFactor;
    } else {
      result = false;
    }
    if (result && gcCount % gcAdjustmentFactor == 0) {
      final int samplesSize = samples.size();
      if (samplesSize > 1.5 * gcTargetSamplesSise) {
        if (gcFactor > 1) {
          final double newGcFactor = 0.71 * gcFactor;
          this.gcFactor = newGcFactor >= 1 ? newGcFactor : 1;
        }
      } else if (samplesSize < 0.67 * gcTargetSamplesSise) {
        this.gcFactor = 1.4 * gcFactor;
      }
    }
    return result;
  }

  private final void gc() {
    if (gcInProgress.compareAndSet(false, true)) {
      try {
        gcCount++;
        final long rightNanos = rightSamplesWindowBoundary();
        gcLastRightSamplesWindowBoundary = rightNanos;
        final long leftNanos = rightNanos - getSamplesIntervalNanos();
        @Nullable
        final Long rightNanosToRemoveTo = samples.floorKey(leftNanos);
        if (rightNanosToRemoveTo != null) {
          samples.subMap(samples.firstKey(), true, rightNanosToRemoveTo, true)
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
  protected NavigableMap<Long, TicksCounter> getSamples() {
    return samples;
  }

  private static final long max(final long v1, final long v2, final Comparator<? super Long> comparator) {
    return comparator.compare(v1, v2) >= 0 ? v1 : v2;
  }
}