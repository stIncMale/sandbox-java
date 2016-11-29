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
   * @param samplesInterval Size of the samples window.
   * @param config Additional configuration parameters.
   */
  public SimpleRateMeter(final long startNanos, final Duration samplesInterval, final RateMeterConfig config) {
    super(startNanos, samplesInterval, config);
    ticksTotalCount = 0;
    samples = new TreeMap<>(NanosComparator.getInstance());
    samples.put(startNanos, new ModifiableLong(0));
  }

  /**
   * Acts like {@link #SimpleRateMeter(long, Duration, RateMeterConfig)} with {@link RateMeterConfig#defaultInstance}
   * as the third argument.
   */
  public SimpleRateMeter(final long startNanos, final Duration samplesInterval) {
    this(startNanos, samplesInterval, RateMeterConfig.defaultInstance());
  }

  @Override
  public final long rightSamplesWindowBoundary() {
    return samples.lastKey();
  }

  @Override
  public final long ticksCount() {
    final long rightNanos = rightSamplesWindowBoundary();
    return internalCount(rightNanos - getSamplesIntervalNanos(), rightNanos);
  }

  @Override
  public final long ticksTotalCount() {
    return ticksTotalCount;
  }

  @Override
  public final void tick(final long count, final long tNanos) {
    checkTNanos(tNanos, "tNanos");
    if (count != 0) {
      final long rightNanos = rightSamplesWindowBoundary();
      final long leftNanos = rightNanos - getSamplesIntervalNanos();
      if (NanosComparator.compare(leftNanos, tNanos) < 0) {//tNanos is within the samples window
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
    return internalRateAverage(rightSamplesWindowBoundary(), getSamplesIntervalNanos());
  }

  @Override
  public final double rateAverage(final long tNanos) {
    checkTNanos(tNanos, "tNanos");
    final long rightNanos = rightSamplesWindowBoundary();
    checkArgument(tNanos >= rightNanos, "tNanos", () -> "Must not be less than " + rightNanos);
    return internalRateAverage(tNanos, getSamplesIntervalNanos());
  }

  @Override
  public final double rate(final long tNanos) {
    checkTNanos(tNanos, "tNanos");
    return internalRate(tNanos, getSamplesIntervalNanos());
  }

  private final double internalRateAverage(final long tNanos, final long unitSizeNanos) {
    final long totalNanos = tNanos - getStartNanos();
    return totalNanos == 0
        ? 0
        : (double) ticksTotalCount / ((double) totalNanos / unitSizeNanos);
  }

  private final double internalRate(final long tNanos, final long unitSizeNanos) {
    final double result;
    final long rightNanos = rightSamplesWindowBoundary();
    if (NanosComparator.compare(tNanos, rightNanos) < 0) {
      result = internalRateAverage(tNanos, unitSizeNanos);
    } else {
      final long samplesIntervalNanos = getSamplesIntervalNanos();
      final long effectiveRightNanos = NanosComparator.max(rightNanos, tNanos);
      final long effectiveLeftNanos = effectiveRightNanos - samplesIntervalNanos;
      final long ticksCount = internalCount(effectiveLeftNanos, effectiveRightNanos);
      result = (unitSizeNanos == samplesIntervalNanos)
          ? ticksCount
          : (double) ticksCount / ((double) samplesIntervalNanos / unitSizeNanos);
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
      final long rightNanos = rightSamplesWindowBoundary();
      final long leftNanos = rightNanos - getSamplesIntervalNanos();
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