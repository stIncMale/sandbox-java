package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import java.util.NavigableMap;
import java.util.TreeMap;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import static stinc.male.sandbox.ratexecutor.Preconditions.checkArgument;

@NotThreadSafe
public final class SimpleRateMeter extends AbstractRateMeter {
  private long ticksTotalCount;
  private final NavigableMap<Long, ModifiableLong> samples;

  /**
   * Constructor.
   *
   * @param startNanos Starting point that is used to calculate elapsed nanoseconds.
   * @param sampleInterval Size of the sample window.
   * @param config Additional configuration parameters.
   */
  public SimpleRateMeter(final long startNanos, final Duration sampleInterval, final RateMeterConfig config) {
    super(startNanos, sampleInterval, config);
    ticksTotalCount = 0;
    samples = new TreeMap<>(NanosComparator.getInstance());
    samples.put(startNanos, new ModifiableLong(0));
  }

  /**
   * Acts like {@link #SimpleRateMeter(long, Duration, RateMeterConfig)} with {@link RateMeterConfig#defaultInstance}
   * as the third argument.
   */
  public SimpleRateMeter(final long startNanos, final Duration sampleInterval) {
    this(startNanos, sampleInterval, RateMeterConfig.defaultInstance());
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
    return ticksTotalCount;
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
        final ModifiableLong newSample = new ModifiableLong(count);
        @Nullable
        final ModifiableLong existingSample = samples.putIfAbsent(tNanos, newSample);
        if (existingSample != null) {//we need to merge samples
          existingSample.value += count;
        }
      }
      ticksTotalCount += count;
      gc(ticksTotalCount);
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
    final long totalNanos = tNanos - getStartNanos();
    return totalNanos == 0
        ? 0
        : (double) ticksTotalCount / ((double) totalNanos / unitSizeNanos);
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
        .mapToLong(sample -> sample.value)
        .sum();
  }

  private final void gc(long counter) {//TODO test; add GC strategies?
    if (counter % 1024 == 0) {
      final long rightNanos = rightSampleWindowBoundary();
      final long leftNanos = rightNanos - getSampleIntervalNanos();
      @Nullable
      final Long rightNanosToRemoveTo = samples.floorKey(leftNanos);
      if (rightNanosToRemoveTo != null) {
        samples.subMap(samples.firstKey(), true, rightNanosToRemoveTo, true)
            .clear();
      }
    }
  }

  private static final class ModifiableLong {
    long value;

    ModifiableLong(final long value) {
      this.value = value;
    }
  }
}