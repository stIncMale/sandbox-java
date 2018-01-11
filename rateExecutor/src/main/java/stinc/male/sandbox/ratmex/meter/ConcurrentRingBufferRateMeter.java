package stinc.male.sandbox.ratmex.meter;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.locks.StampedLock;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * This thread-safe implementation uses a concurrent ring buffer with the underlying {@link AtomikLongArray}
 * to store and access a samples history.
 * <p>
 * There are two modes:
 * <ul>
 * <li>strict (default) - {@link ConcurrentRateMeterConfig#isStrictTick()} is true.</li>
 * <li>relaxed - {@link ConcurrentRateMeterConfig#isStrictTick()} is false.
 * Displays much better performance in terms of both throughput and latency,
 * and does not {@linkplain ConcurrentRateMeterStats#failedAccuracyEventsCountForTick() fail to correctly register} ticks
 * with {@link #tick(long, long)} in reasonable practical situations in spite of allowing such incorrectness in theory.
 * This is a recommended mode.</li>
 * </ul>
 * <p>
 * <i>Advantages</i><br>
 * <ul>
 * <li>Unlike {@link ConcurrentNavigableMapRateMeter}, this implementation does not produces garbage,
 * unless customizable tools that are being used by the implementation produce garbage
 * (e.g. {@link StampedLockStrategy} produces garbage because {@link StampedLock} does).</li>
 * <li>Unlike {@link ConcurrentNavigableMapRateMeter}, this implementation can tolerate
 * {@link RateMeterConfig#getHistoryLength() long samples history}.</li>
 * <li>Unlike {@link ConcurrentNavigableMapRateMeter}, this implementation takes advantage of memory locality of data.</li>
 * </ul>
 * <p>
 * <i>Disadvantages</i><br>
 * <ul>
 * <li>Unlike {@link ConcurrentNavigableMapRateMeter}, this implementation can not tolerate a large ratio of
 * {@link RateMeter#getSamplesInterval()} and {@link RateMeter#getTimeSensitivity()}.
 * The reason for this is that a ring buffer requires all objects representing samples to always exist,
 * and if the number of such objects (which is the same as the aforementioned ratio) is large,
 * it can have a substantial negative effect on performance.</li>
 * </ul>
 */
@ThreadSafe
public final class ConcurrentRingBufferRateMeter extends AbstractRingBufferRateMeter<ConcurrentRateMeterStats, ConcurrentRateMeterConfig> {
  private static final ConcurrentRateMeterConfig defaultConfig = ConcurrentRateMeterConfig.newBuilder()
      .build();

  @Nullable
  private final DefaultConcurrentRingBufferRateMeterStats stats;

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
    stats = config.isCollectStats() ? new DefaultConcurrentRingBufferRateMeterStats() : null;
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
  protected final void registerFailedAccuracyEventForTick() {
    if (stats != null) {
      stats.registerFailedAccuracyEventForTick();
    }
  }
}