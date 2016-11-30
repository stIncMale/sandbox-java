package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import java.util.TreeMap;
import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public class AccurateRateMeter extends AbstractNavigableMapRateMeter {
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
}