package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import java.util.concurrent.ConcurrentSkipListMap;
import javax.annotation.concurrent.ThreadSafe;

/**
 * This implementation of {@link RateMeter} is not linearizable and may produce inaccurate results if used concurrently
 * (see {@link RateMeter} for details).
 */
@ThreadSafe
public class ConcurrentNavigableMapRateMeter extends AbstractNavigableMapRateMeter<ConcurrentSkipListMap<Long, TicksCounter>> {
  private static final RateMeterConfig defaultConfigInstance = RateMeterConfig.newBuilder()
          .setTicksCounterSupplier(LongAdderTicksCounter::new)
          .build();

  /**
   * @return A reasonable configuration.
   */
  public static final RateMeterConfig defaultConfig() {
    return defaultConfigInstance;
  }

  /**
   * Constructor.
   *
   * @param startNanos Starting point that is used to calculate elapsed nanoseconds.
   * @param samplesInterval Size of the samples window.
   * @param config Additional configuration parameters.
   */
  public ConcurrentNavigableMapRateMeter(final long startNanos, final Duration samplesInterval, final RateMeterConfig config) {
    super(startNanos, samplesInterval, config, () -> new ConcurrentSkipListMap<>(NanosComparator.instance()), false);
  }

  /**
   * Acts like {@link #ConcurrentNavigableMapRateMeter(long, Duration, RateMeterConfig)} with {@link #defaultConfig()}
   * as the third argument.
   */
  public ConcurrentNavigableMapRateMeter(final long startNanos, final Duration samplesInterval) {
    this(startNanos, samplesInterval, defaultConfigInstance);
  }
}