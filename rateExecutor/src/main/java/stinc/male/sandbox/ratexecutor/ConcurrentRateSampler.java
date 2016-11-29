package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import static stinc.male.sandbox.ratexecutor.Preconditions.checkArgument;

@ThreadSafe
public final class ConcurrentRateSampler extends AbstractRateSampler {
  private final AtomicLong aTicksTotalCount;
  private final ConcurrentNavigableMap<Long, AtomicLong> samples;
  private final AtomicBoolean aGcFlag;

  /**
   * Constructor.
   *
   * @param startNanos Starting point that is used to calculate elapsed nanoseconds.
   * @param sampleInterval Size of the sample window.
   * @param config Additional configuration parameters.
   */
  public ConcurrentRateSampler(final long startNanos, final Duration sampleInterval, final RateSamplerConfig config) {
    super(startNanos, sampleInterval, config);
    aTicksTotalCount = new AtomicLong();
    samples = new ConcurrentSkipListMap<>(NanosComparator.getInstance());
    samples.put(startNanos, new AtomicLong());//TODO why?
    aGcFlag = new AtomicBoolean();
  }

  /**
   * Acts like {@link #ConcurrentRateSampler(long, Duration, RateSamplerConfig)} with {@link RateSamplerConfig#defaultInstance}
   * as the third argument.
   */
  public ConcurrentRateSampler(final long startNanos, final Duration sampleInterval) {
    this(startNanos, sampleInterval, RateSamplerConfig.defaultInstance());
  }

  @Override
  public final long rightSampleWindowBoundary() {
    return samples.lastKey();
  }

  @Override
  public final long ticksCount() {
    final long rightNanos = rightSampleWindowBoundary();
    return internalCount(rightNanos - getSampleIntervalNanos(), rightNanos);
  }

  @Override
  public final long ticksTotalCount() {
    return aTicksTotalCount.get();
  }

  @Override
  public final void tick(final long count, final long tNanos) {
    final long startNanos = getStartNanos();
    checkArgument(NanosComparator.compare(tNanos, startNanos) >= 0, "tNanos", () -> "Must not be less than " + startNanos);
    checkTNanos(tNanos, "tNanos");
    if (count != 0) {
      final long rightNanos = rightSampleWindowBoundary();
      final long leftNanos = rightNanos - getSampleIntervalNanos();
      if (NanosComparator.compare(leftNanos, tNanos) < 0) {//tNanos is within the sample window
        final AtomicLong newSample = new AtomicLong(count);
        @Nullable
        final AtomicLong existingSample = samples.putIfAbsent(tNanos, newSample);
        if (existingSample != null) {//we need to merge samples
          existingSample.addAndGet(count);
        }
      }
      final long totalTicksCount = aTicksTotalCount.addAndGet(count);
      gc(totalTicksCount);
    }
  }

  @Override
  public final double rateAverage() {
    return internalRateAverage(rightSampleWindowBoundary(), getSampleIntervalNanos());
  }

  @Override
  public final double rateAverage(final long tNanos) {
    checkTNanos(tNanos, "tNanos");
    final long rightNanos = rightSampleWindowBoundary();
    checkArgument(tNanos >= rightNanos, "tNanos", () -> "Must not be less than " + rightNanos);
    return internalRateAverage(tNanos, getSampleIntervalNanos());
  }

  @Override
  public final double rate(final long tNanos) {
    checkTNanos(tNanos, "tNanos");
    return internalRate(tNanos, getSampleIntervalNanos());
  }

  private final double internalRateAverage(final long tNanos, final long unitSizeNanos) {
    final long totalTicksCount = aTicksTotalCount.get();
    final long totalNanos = tNanos - getStartNanos();
    return totalNanos == 0
        ? 0
        : (double) totalTicksCount / ((double) totalNanos / unitSizeNanos);
  }

  private final double internalRate(final long tNanos, final long unitSizeNanos) {
    final double result;
    final long rightNanos = rightSampleWindowBoundary();
    if (NanosComparator.compare(tNanos, rightNanos) < 0) {
      result = internalRateAverage(tNanos, unitSizeNanos);
    } else {
      final long sampleIntervalNanos = getSampleIntervalNanos();
      final long effectiveRightNanos = NanosComparator.max(rightNanos, tNanos);
      final long effectiveLeftNanos = effectiveRightNanos - sampleIntervalNanos;
      final long ticksCount = internalCount(effectiveLeftNanos, effectiveRightNanos);
      result = (unitSizeNanos == sampleIntervalNanos)
          ? ticksCount
          : (double) ticksCount / ((double) sampleIntervalNanos / unitSizeNanos);
    }
    return result;
  }

  private final long internalCount(final long fromExclusiveNanos, final long toInclusiveNanos) {
    return samples.subMap(fromExclusiveNanos, false, toInclusiveNanos, true)
        .values()
        .stream()
        .mapToLong(AtomicLong::get)
        .sum();
  }

  private final void gc(long counter) {//TODO test; adaptive 1024
    if (counter % 1024 == 0) {
      if (aGcFlag.compareAndSet(false, true)) {
        try {
          final long rightNanos = rightSampleWindowBoundary();
          final long leftNanos = rightNanos - getSampleIntervalNanos();
          @Nullable
          final Long rightNanosToRemoveTo = samples.floorKey(leftNanos);
          if (rightNanosToRemoveTo != null) {
            samples.subMap(samples.firstKey(), true, rightNanosToRemoveTo, true)
                .clear();
          }
        } finally {
          aGcFlag.set(false);
        }
      }
    }
  }
}