package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import java.util.concurrent.ConcurrentSkipListMap;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class ConcurrentSkipListMapRateMeter extends AbstractNavigableMapRateMeter<ConcurrentSkipListMap<Long, TicksCounter>> {
  /**
   * Constructor.
   *
   * @param startNanos Starting point that is used to calculate elapsed nanoseconds.
   * @param samplesInterval Size of the samples window.
   * @param config Additional configuration parameters.
   */
  public ConcurrentSkipListMapRateMeter(final long startNanos, final Duration samplesInterval, final RateMeterConfig config) {
    super(startNanos, samplesInterval, config, () -> new ConcurrentSkipListMap<>(NanosComparator.getInstance()));
  }

  /**
   * Acts like {@link #ConcurrentSkipListMapRateMeter(long, Duration, RateMeterConfig)} with {@link RateMeterConfig#defaultInstance()}
   * as the third argument.
   */
  public ConcurrentSkipListMapRateMeter(final long startNanos, final Duration samplesInterval) {
    this(startNanos, samplesInterval, RateMeterConfig.defaultInstance());
  }
}