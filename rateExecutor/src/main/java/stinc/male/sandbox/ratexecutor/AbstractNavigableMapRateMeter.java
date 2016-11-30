package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import java.util.Comparator;
import java.util.NavigableMap;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import static stinc.male.sandbox.ratexecutor.Preconditions.checkNotNull;
import static stinc.male.sandbox.ratexecutor.RateMeterMath.convertRate;

public abstract class AbstractNavigableMapRateMeter extends AbstractRateMeter {
  private final NavigableMap<Long, TicksCounter> samples;

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
      if (gcRequired()) {
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

  protected boolean gcRequired() {
    return ticksTotalCount() % 1024 == 0;//TODO test; GC strategies, adaptive 1024, use rightNanos - startNanos instead of ticksTotal?
  }

  /**
   * This method is called by {@link #tick(long, long)} if {@link #gcRequired()} is {@code true}.
   */
  protected void gc() {
    final long rightNanos = rightSamplesWindowBoundary();
    final long leftNanos = rightNanos - getSamplesIntervalNanos();
    final NavigableMap<Long, TicksCounter> samples = getSamples();
    @Nullable
    final Long rightNanosToRemoveTo = samples.floorKey(leftNanos);
    if (rightNanosToRemoveTo != null) {
      samples.subMap(samples.firstKey(), true, rightNanosToRemoveTo, true)
          .clear();
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