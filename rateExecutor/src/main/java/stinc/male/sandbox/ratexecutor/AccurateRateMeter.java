package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import java.util.NavigableMap;
import java.util.TreeMap;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public final class AccurateRateMeter extends AbstractNavigableMapRateMeter {
  /**
   * Constructor.
   *
   * @param startNanos Starting point that is used to calculate elapsed nanoseconds.
   * @param samplesInterval Size of the samples window.
   * @param config Additional configuration parameters.
   */
  public AccurateRateMeter(final long startNanos, final Duration samplesInterval, final RateMeterConfig config) {
    super(startNanos, samplesInterval, config, () -> new TreeMap<>(NanosComparator.getInstance()));
  }

  /**
   * Acts like {@link #AccurateRateMeter(long, Duration, RateMeterConfig)} with {@link RateMeterConfig#defaultInstance()}
   * as the third argument.
   */
  public AccurateRateMeter(final long startNanos, final Duration samplesInterval) {
    this(startNanos, samplesInterval, RateMeterConfig.defaultInstance());
  }

  @Override
  protected final void doGc() {
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
}