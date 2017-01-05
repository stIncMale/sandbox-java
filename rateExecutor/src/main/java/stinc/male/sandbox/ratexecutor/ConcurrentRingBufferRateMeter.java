package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import javax.annotation.concurrent.ThreadSafe;

/**
 * This implementation of {@link RateMeter} is not linearizable and may produce inaccurate results if used concurrently
 * (see {@link RateMeter} for details).
 */
@ThreadSafe
public class ConcurrentRingBufferRateMeter extends AbstractRingBufferRateMeter<ConcurrentLongArray> {
  private static final ConcurrentRingBufferRateMeterConfig defaultConfigInstance = ConcurrentRingBufferRateMeterConfig.newBuilder()
      .setTicksCounterSupplier(LongAdderTicksCounter::new)
      .build();

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