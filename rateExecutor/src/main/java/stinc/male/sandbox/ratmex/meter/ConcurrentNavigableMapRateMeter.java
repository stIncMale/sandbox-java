package stinc.male.sandbox.ratmex.meter;

import java.time.Duration;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import javax.annotation.concurrent.ThreadSafe;
import stinc.male.sandbox.ratmex.NanosComparator;

/**
 * This thread-safe implementation uses a concurrent {@link NavigableMap} (currently {@link ConcurrentSkipListMap})
 * to store and access a samples history.
 * <p>
 * <i>Advantages</i><br>
 * <ul>
 * <li>Unlike {@link ConcurrentRingBufferRateMeter}, this implementation tolerates a large ratio of
 * {@link RateMeter#getSamplesInterval()} and {@link RateMeter#getTimeSensitivity()}.
 * The reason for this is that it only creates objects representing samples when it is necessary,
 * hence potentially reducing the number of samples that must be added up to count the {@linkplain #ticksCount() current ticks}.</li>
 * </ul>
 * <p>
 * <i>Disadvantages</i><br>
 * <ul>
 * <li>Unlike {@link ConcurrentRingBufferRateMeter}, this implementation produces garbage.</li>
 * <li>Unlike {@link ConcurrentRingBufferRateMeter}, this implementation displays performance
 * that diminishes with growth of {@link RateMeterConfig#getHistoryLength() samples history length}.</li>
 * <li>Unlike {@link ConcurrentRingBufferRateMeter}, this implementation does not benefit from the idea of of memory locality of data.</li>
 * </ul>
 */
@ThreadSafe
public final class ConcurrentNavigableMapRateMeter extends AbstractNavigableMapRateMeter<ConcurrentRateMeterConfig> {
  private static final ConcurrentRateMeterConfig defaultConfig = ConcurrentRateMeterConfig.newBuilder()
      .build();

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
  public ConcurrentNavigableMapRateMeter(final long startNanos, final Duration samplesInterval, final ConcurrentRateMeterConfig config) {
    super(startNanos, samplesInterval, config, () -> new ConcurrentSkipListMap<>(NanosComparator.instance()), false);
  }

  /**
   * This constructor is equivalent to {@link #ConcurrentNavigableMapRateMeter(long, Duration, ConcurrentRateMeterConfig)}
   * with {@link #defaultConfig()} as the third argument.
   */
  public ConcurrentNavigableMapRateMeter(final long startNanos, final Duration samplesInterval) {
    this(startNanos, samplesInterval, defaultConfig);
  }
}