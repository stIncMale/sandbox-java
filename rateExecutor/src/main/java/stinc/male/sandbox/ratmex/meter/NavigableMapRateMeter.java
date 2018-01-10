package stinc.male.sandbox.ratmex.meter;

import java.time.Duration;
import java.util.NavigableMap;
import java.util.TreeMap;
import javax.annotation.concurrent.NotThreadSafe;
import stinc.male.sandbox.ratmex.NanosComparator;

/**
 * This implementation uses {@link NavigableMap} to store and access a samples history.
 * <p>
 * <i>Advantages</i><br>
 * <ul>
 * <li>Unlike {@link RingBufferRateMeter}, this implementation tolerates a large ratio of
 * {@link RateMeter#getSamplesInterval()} and {@link RateMeter#getTimeSensitivity()}.
 * The reason for this is that it only creates objects representing samples when it is necessary,
 * hence potentially reducing the number of samples that must be added up to count the {@linkplain #ticksCount() current ticks}.</li>
 * </ul>
 * <p>
 * <i>Disadvantages</i><br>
 * <ul>
 * <li>Unlike {@link RingBufferRateMeter}, this implementation produces garbage.</li>
 * <li>Unlike {@link RingBufferRateMeter}, this implementation displays performance
 * that diminishes with growth of {@link ConcurrentRateMeterConfig#getHistoryLength() samples history length}.</li>
 * <li>Unlike {@link RingBufferRateMeter}, this implementation does not benefit from the idea of of memory locality of data.</li>
 * </ul>
 */
@NotThreadSafe
public final class NavigableMapRateMeter extends AbstractNavigableMapRateMeter<ConcurrentRateMeterConfig> {
  private static final ConcurrentRateMeterConfig defaultConfig;

  static {
    ConcurrentRateMeterConfig.Builder configBuilder = ConcurrentRateMeterConfig.newBuilder();
    configBuilder.setTicksCounterSupplier(LongTicksCounter::new)
        .setHistoryLength(2)
        .build();
    defaultConfig = configBuilder.build();
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
  public NavigableMapRateMeter(final long startNanos, final Duration samplesInterval, final RateMeterConfig config) {
    this(startNanos, samplesInterval, ConcurrentRateMeterConfig.newBuilder(defaultConfig)
        .set(config)
        .build());
  }

  /**
   * This constructor is equivalent to {@link #NavigableMapRateMeter(long, Duration, RateMeterConfig)}
   * with {@link #defaultConfig()} as the third argument.
   */
  public NavigableMapRateMeter(final long startNanos, final Duration samplesInterval) {
    this(startNanos, samplesInterval, defaultConfig);
  }

  private NavigableMapRateMeter(final long startNanos, final Duration samplesInterval, final ConcurrentRateMeterConfig config) {
    super(startNanos, samplesInterval, config, () -> new TreeMap<>(NanosComparator.instance()), true);
  }
}