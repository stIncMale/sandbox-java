package stinc.male.sandbox.ratmex.meter;

import java.time.Duration;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A thread-safe implementation of {@link AbstractRingBufferRateMeter},
 * which uses {@link AtomikLongArray} implementation of {@link LongArray} (this might be changed in the future).
 * <p>
 * There are two modes:
 * <ul>
 * <li>strict (default) - {@link ConcurrentRateMeterConfig#isStrictTick()} is true.</li>
 * <li>relaxed (recommended) - {@link ConcurrentRateMeterConfig#isStrictTick()} is false.
 * Displays much better performance in terms of both throughput and latency,
 * and does not {@linkplain ConcurrentRateMeterStats#incorrectlyRegisteredTicksEventsCount() fail to correctly register} ticks
 * with {@link #tick(long, long)} in reasonable practical situations in spite of allowing such incorrectness in theory.</li>
 * </ul>
 */
//TODO check how much garbage this implementation generates
@ThreadSafe
public final class ConcurrentRingBufferRateMeter extends AbstractRingBufferRateMeter<ConcurrentRateMeterStats, ConcurrentRateMeterConfig> {
  private static final ConcurrentRateMeterConfig defaultConfig = ConcurrentRateMeterConfig.newBuilder()
      .build();

  @Nullable
  private final ConcurrentRingBufferRateMeterStats stats;

  /**
   * @return A default configuration, which is the default {@link ConcurrentRateMeterConfig}.
   */
  public static final ConcurrentRateMeterConfig defaultConfig() {
    return defaultConfig;
  }

  /**
   * @param startNanos A {@linkplain #getStartNanos() starting point} that is used to calculate elapsed time in nanoseconds (tNanos).
   * @param samplesInterval A size of the {@linkplain #getSamplesInterval() samples window}.
   * Must not be null, see {@link RateMeter} for valid values.
   * @param config An additional {@linkplain #getConfig() configuration}. Must not be null.
   */
  public ConcurrentRingBufferRateMeter(final long startNanos, final Duration samplesInterval, final ConcurrentRateMeterConfig config) {
    super(startNanos, samplesInterval, config, AtomikLongArray::new, false);
    stats = config.isCollectStats() ? new ConcurrentRingBufferRateMeterStats() : null;
  }

  /**
   * This constructor is equivalent to {@link #ConcurrentRingBufferRateMeter(long, Duration, ConcurrentRateMeterConfig)}
   * with {@link #defaultConfig()} as the third argument.
   */
  public ConcurrentRingBufferRateMeter(final long startNanos, final Duration samplesInterval) {
    this(startNanos, samplesInterval, defaultConfig);
  }

  /**
   * @return An {@linkplain Optional#empty() empty} {@link Optional}.
   */
  @Override
  public final Optional<ConcurrentRateMeterStats> stats() {
    return Optional.ofNullable(stats);
  }

  @Override
  protected final void registerIncorrectlyRegisteredTicksEvent() {
    if (stats != null) {
      stats.registerIncorrectlyRegisteredTicksEvent();
    }
  }
}