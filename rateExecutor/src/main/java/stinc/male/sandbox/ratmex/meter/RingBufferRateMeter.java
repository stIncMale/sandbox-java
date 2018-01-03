package stinc.male.sandbox.ratmex.meter;

import java.time.Duration;
import java.util.Optional;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * This implementation uses a ring buffer to store and access a samples history.
 * <p>
 * <i>Advantages</i><br>
 * Unlike {@link NavigableMapRateMeter}, this implementation does not produces garbage.
 * <p>
 * <i>Disadvantages</i><br>
 * Unlike {@link NavigableMapRateMeter}, this implementation can not tolerate a large ratio of
 * {@link RateMeter#getSamplesInterval()} and {@link RateMeter#getTimeSensitivity()}.
 * The reason for this is that a ring buffer requires all objects representing samples to always exist,
 * and if the number of such objects (which is the same as the aforementioned ratio) is large,
 * it can have a substantial negative effect on performance.
 */
@NotThreadSafe
public final class RingBufferRateMeter extends AbstractRingBufferRateMeter<Void, ConcurrentRateMeterConfig> {
  private static final ConcurrentRateMeterConfig defaultConfig;

  static {
    final ConcurrentRateMeterConfig.Builder defaultConfigBuilder = ConcurrentRateMeterConfig.newBuilder();
    defaultConfigBuilder.setTicksCounterSupplier(LongTicksCounter::new)
        .setHistoryLength(2);
    defaultConfig = defaultConfigBuilder.build();
  }

  /**
   * @return A default configuration.
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
    this(startNanos, samplesInterval, ConcurrentRateMeterConfig.newBuilder(defaultConfig)
        .set(config)
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
    super(startNanos, samplesInterval, config, SequentialLongArray::new, true);
  }

  /**
   * @return An {@linkplain Optional#empty() empty} {@link Optional}.
   */
  @Override
  public final Optional<Void> stats() {
    return Optional.empty();
  }
}