package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import java.util.NavigableMap;
import java.util.TreeMap;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import static stinc.male.sandbox.ratexecutor.Preconditions.checkArgument;
import static stinc.male.sandbox.ratexecutor.Preconditions.checkNotNull;

@NotThreadSafe
public final class SimpleRateSampler implements RateSampler {//TODO abstract class for Simple and Concurrent implementations
  private final Nanos startNanos;
  private final Duration sampleInterval;
  private final long sampleIntervalNanos;
  private long totalTicksCount;
  private final NavigableMap<Long, ModifiableLong> samples;

  /**
   * Constructor.
   *
   * @param startNanos Starting point that is used to calculate elapsed nanoseconds.
   * @param sampleInterval Size of the sample window.
   */
  public SimpleRateSampler(final long startNanos, final Duration sampleInterval) {
    checkArgument(startNanos != Double.NaN, "startNanos", "Must not be NaN");
    checkNotNull(sampleInterval, "sampleInterval");
    checkArgument(!sampleInterval.isZero(), "sampleInterval", "Must not be zero");
    checkArgument(!sampleInterval.isNegative(), "sampleInterval", "Must be positive");
    this.sampleInterval = sampleInterval;
    sampleIntervalNanos = sampleInterval.toNanos();
    this.startNanos = new Nanos(startNanos, sampleIntervalNanos);
    totalTicksCount = 0;
    samples = new TreeMap<>(NanosComparator.getInstance());
    samples.put(startNanos, new ModifiableLong(0));
  }

  @Override
  public final long getStartNanos() {
    return startNanos.value;
  }

  @Override
  public final Duration getSampleInterval() {
    return sampleInterval;
  }

  @Override
  public final long rightSampleWindowBoundary() {
    return samples.lastKey();
  }

  @Override
  public final long ticksCount() {
    final long rightNanos = samples.lastKey();
    return internalCount(rightNanos - sampleIntervalNanos, rightNanos);
  }

  @Override
  public final long ticksTotalCount() {
    return totalTicksCount;
  }

  @Override
  public final void tick(final long count, final long tNanos) {
    checkArgument(NanosComparator.compare(tNanos, startNanos.value) >= 0, "tNanos", () -> "Must not be less than " + startNanos.value);
    startNanos.check(tNanos, "tNanos");
    if (count != 0) {
      final ModifiableLong newSample = new ModifiableLong(count);
      @Nullable
      final ModifiableLong existingSample = samples.putIfAbsent(tNanos, newSample);
      if (existingSample != null) {//we need to merge samples
        existingSample.value += count;
      }
      totalTicksCount += count;
      gc(totalTicksCount);
    }
  }

  @Override
  public final double rateAverage() {
    return internalRateAverage(samples.lastKey(), sampleIntervalNanos);
  }

  @Override
  public final double rateAverage(final Duration unit) {
    checkNotNull(unit, "unit");
    checkArgument(!unit.isZero(), "sampleInterval", "Must not be zero");
    checkArgument(!unit.isNegative(), "sampleInterval", "Must be positive");
    return rateAverage() / ((double) sampleIntervalNanos / unit.toNanos());
  }

  @Override
  public final double rateAverage(final long tNanos) {
    startNanos.check(tNanos, "tNanos");
    final long rightNanos = samples.lastKey();
    checkArgument(tNanos >= rightNanos, "tNanos", () -> "Must not be less than " + rightNanos);
    return internalRateAverage(tNanos, sampleIntervalNanos);
  }

  @Override
  public final double rateAverage(final long tNanos, final Duration unit) {
    checkNotNull(unit, "unit");
    checkArgument(!unit.isZero(), "sampleInterval", "Must not be zero");
    checkArgument(!unit.isNegative(), "sampleInterval", "Must be positive");
    return rateAverage(tNanos) / ((double) sampleIntervalNanos / unit.toNanos());
  }

  @Override
  public final double rate(final Duration unit) {
    checkNotNull(unit, "unit");
    checkArgument(!unit.isZero(), "sampleInterval", "Must not be zero");
    checkArgument(!unit.isNegative(), "sampleInterval", "Must be positive");
    return rate() / ((double) sampleIntervalNanos / unit.toNanos());
  }

  @Override
  public final double rate(final long tNanos, final Duration unit) {
    checkNotNull(unit, "unit");
    checkArgument(!unit.isZero(), "sampleInterval", "Must not be zero");
    checkArgument(!unit.isNegative(), "sampleInterval", "Must be positive");
    return rate(tNanos) / ((double) sampleIntervalNanos / unit.toNanos());
  }

  @Override
  public final double rate(final long tNanos) {
    startNanos.check(tNanos, "tNanos");
    return internalRate(tNanos, sampleIntervalNanos);
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName()
        + "(startNanos=" + startNanos.value
        + ", sampleIntervalNanos=" + sampleIntervalNanos
        + ')';
  }

  private final double internalRateAverage(final long tNanos, final long unitSizeNanos) {
    final long totalNanos = tNanos - startNanos.value;
    return totalNanos == 0
        ? 0
        : (double) totalTicksCount / ((double) totalNanos / unitSizeNanos);
  }

  private final double internalRate(final long tNanos, final long unitSizeNanos) {
    final double result;
    final long rightNanos = samples.lastKey();
    if (NanosComparator.compare(tNanos, rightNanos) < 0) {
      result = internalRateAverage(tNanos, unitSizeNanos);
    } else {
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
        .reduce(0, Math::addExact);
  }

  private final void gc(long counter) {//TODO test
    if (counter % 1024 == 0) {
      final long rightNanos = samples.lastKey();
      final long leftNanos = rightNanos - sampleIntervalNanos;
      @Nullable
      final Long rightNanosToRemoveTo = samples.floorKey(leftNanos);
      if (rightNanosToRemoveTo != null) {
        samples.subMap(samples.firstKey(), true, rightNanosToRemoveTo, true)
            .clear();
      }
    }
  }

  private static final class Nanos {
    private final long value;
    private final long l1;
    private final long r1;
    private final long l2;
    private final long r2;

    Nanos(final long value, final long sampleIntervalNanos) {
      this.value = value;
      if ((value - sampleIntervalNanos) <= 0) {
        l1 = value;
        r1 = value - sampleIntervalNanos + Long.MAX_VALUE;
        l2 = 0;
        r2 = 0;
      } else {
        l1 = value;
        r1 = Long.MAX_VALUE;
        l2 = Long.MIN_VALUE;
        r2 = Long.MIN_VALUE + value - sampleIntervalNanos - 1;
      }
    }

    final void check(final long nanos, final String paramName) {
      if (value <= 0) {
        checkArgument(NanosComparator.compare(l1, nanos) <= 0 && NanosComparator.compare(nanos, r1) <= 0,
            paramName, () -> String.format("Must be in [%s; %s]", l1, r1));
      } else {
        checkArgument((NanosComparator.compare(l1, nanos) <= 0 && NanosComparator.compare(nanos, r1) <= 0)
                || (NanosComparator.compare(l2, nanos) <= 0 && NanosComparator.compare(nanos, r2) <= 0),
            paramName, () -> String.format("Must be in [%s; %s]\u222a[%s; %s]", l1, r1, l2, r2));
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