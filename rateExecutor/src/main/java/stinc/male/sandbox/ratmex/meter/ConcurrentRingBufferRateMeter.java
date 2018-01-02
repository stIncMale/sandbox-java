package stinc.male.sandbox.ratmex.meter;

import java.time.Duration;
import javax.annotation.concurrent.ThreadSafe;

/**
 * This implementation uses a ring buffer to store and access a samples history.
 * <p>
 * <i>Advantages</i><br>
 * Unlike {@link ConcurrentNavigableMapRateMeter}, this implementation does not produces garbage.
 * <p>
 * <i>Disadvantages</i><br>
 * Unlike {@link ConcurrentNavigableMapRateMeter}, this implementation can not tolerate a large ratio of
 * {@link RateMeter#getSamplesInterval()} and {@link RateMeter#getTimeSensitivity()}.
 * The reason for this is that a ring buffer requires all objects representing samples to always exist,
 * and if the number of such objects (which is the same as the aforementioned ratio) is large,
 * it can have a substantial negative effect on performance.
 */
@ThreadSafe
public final class ConcurrentRingBufferRateMeter extends AbstractRingBufferRateMeter<ConcurrentRingBufferRateMeterConfig> {
  private static final ConcurrentRingBufferRateMeterConfig defaultConfig;

  static {
    final ConcurrentRingBufferRateMeterConfig.Builder defaultConfigBuilder = ConcurrentRingBufferRateMeterConfig.newBuilder();
    defaultConfigBuilder.setTicksCounterSupplier(LongAdderTicksCounter::new);
    defaultConfigBuilder.setWaitStrategySupplier(ParkWaitStrategy::instance);
    defaultConfigBuilder.setLockStrategySupplier(StampedLockStrategy::new);
    defaultConfig = defaultConfigBuilder.build();
  }

  /**
   * @return A default configuration.
   */
  public static final ConcurrentRingBufferRateMeterConfig defaultConfig() {
    return defaultConfig;
  }

  /**
   * @param startNanos A {@linkplain #getStartNanos() starting point} that is used to calculate elapsed time in nanoseconds (tNanos).
   * @param samplesInterval A size of the {@linkplain #getSamplesInterval() samples window}.
   * Must not be null, see {@link RateMeter} for valid values.
   * @param config An additional {@linkplain #getConfig() configuration}. Must not be null.
   */
  public ConcurrentRingBufferRateMeter(final long startNanos, final Duration samplesInterval, final ConcurrentRingBufferRateMeterConfig config) {
    super(startNanos, samplesInterval, config, ConcurrentLongArray::new, false);
  }

  /**
   * This constructor is equivalent to {@link #ConcurrentRingBufferRateMeter(long, Duration, ConcurrentRingBufferRateMeterConfig)}
   * with {@link #defaultConfig()} as the third argument.
   */
  public ConcurrentRingBufferRateMeter(final long startNanos, final Duration samplesInterval) {
    super(startNanos, samplesInterval, defaultConfig, ConcurrentLongArray::new, false);
  }
}