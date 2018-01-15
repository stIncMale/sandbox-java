package stinc.male.sandbox.ratmex.meter;

import java.time.Duration;
import java.util.Optional;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * A not thread-safe implementation of {@link AbstractRingBufferRateMeter},
 * which uses {@link PlainLongArray} implementation of {@link LongArray} (this might be changed in the future).
 */
@NotThreadSafe
public final class RingBufferRateMeter extends AbstractRingBufferRateMeter<Void, ConcurrentRateMeterConfig> {
  private static final ConcurrentRateMeterConfig defaultConfig =
      ((ConcurrentRateMeterConfig.Builder)ConcurrentRateMeterConfig.newBuilder()
          .set(RateMeterConfig.newBuilder()
              .build()))//set back RateMeterConfig defaults because we are using ConcurrentRateMeterConfig for a sequential case
          .build();

  /**
   * @return A default configuration, which is the default {@link RateMeterConfig}.
   */
  public static final RateMeterConfig defaultConfig() {
    return defaultConfig;
  }

  /**
   * @param startNanos A {@linkplain #getStartNanos() starting point} that is used to calculate elapsed time in nanoseconds (tNanos).
   * @param samplesInterval A size of the {@linkplain #getSamplesInterval() samples window}.
   * Must not be null, see {@link RateMeter} for valid values.
   * @param config An additional {@linkplain #getConfig() configuration}. Must not be null.
   */
  public RingBufferRateMeter(final long startNanos, final Duration samplesInterval, final RateMeterConfig config) {
    this(startNanos, samplesInterval, ((ConcurrentRateMeterConfig.Builder)defaultConfig().toBuilder()
        .set(config))
        .build());
  }

  /**
   * This constructor is equivalent to {@link #RingBufferRateMeter(long, Duration, RateMeterConfig)}
   * with {@link #defaultConfig()} as the third argument.
   */
  public RingBufferRateMeter(final long startNanos, final Duration samplesInterval) {
    this(startNanos, samplesInterval, defaultConfig);
  }

  private RingBufferRateMeter(final long startNanos, final Duration samplesInterval, final ConcurrentRateMeterConfig config) {
    super(startNanos, samplesInterval, config, PlainLongArray::new, true);
  }

  /**
   * @return An {@linkplain Optional#empty() empty} {@link Optional}.
   */
  @Override
  public final Optional<Void> stats() {
    return Optional.empty();
  }
}