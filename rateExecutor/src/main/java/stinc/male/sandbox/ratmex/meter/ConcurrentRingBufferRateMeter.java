package stinc.male.sandbox.ratmex.meter;

import java.time.Duration;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class ConcurrentRingBufferRateMeter extends AbstractRingBufferRateMeter<ConcurrentRingBufferRateMeterConfig, ConcurrentLongArray> {
  private static final ConcurrentRingBufferRateMeterConfig defaultConfigInstance;

  static {
    final ConcurrentRingBufferRateMeterConfig.Builder defaultConfigBuilder = ConcurrentRingBufferRateMeterConfig.newBuilder();
    defaultConfigBuilder.setTicksCounterSupplier(LongAdderTicksCounter::new);
    defaultConfigBuilder.setWaitStrategySupplier(ParkWaitStrategy::instance);
    defaultConfigBuilder.setLockStrategySupplier(StampedLockStrategy::new);
    defaultConfigInstance = defaultConfigBuilder.build();
  }

  /**
   * @return A reasonable configuration.
   */
  public static final ConcurrentRingBufferRateMeterConfig defaultConfig() {
    return defaultConfigInstance;
  }

  /**
   * @param startNanos Starting point that is used to calculate elapsed nanoseconds.
   * @param samplesInterval Size of the samples window.
   * @param config Additional configuration parameters.
   */
  public ConcurrentRingBufferRateMeter(
      final long startNanos,
      final Duration samplesInterval,
      final ConcurrentRingBufferRateMeterConfig config) {
    super(
        startNanos,
        samplesInterval,
        config,
        ConcurrentLongArray::new,
        false);
  }

  /**
   * Acts like {@link #ConcurrentRingBufferRateMeter(long, Duration, ConcurrentRingBufferRateMeterConfig)} with {@link #defaultConfig()}
   * as the third argument.
   */
  public ConcurrentRingBufferRateMeter(
      final long startNanos,
      final Duration samplesInterval) {
    super(
        startNanos,
        samplesInterval,
        defaultConfigInstance,
        ConcurrentLongArray::new,
        false);
  }
}