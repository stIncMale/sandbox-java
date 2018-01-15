package stinc.male.sandbox.ratmex.meter;

import java.time.Duration;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import javax.annotation.concurrent.ThreadSafe;
import stinc.male.sandbox.ratmex.NanosComparator;

/**
 * A thread-safe implementation of {@link AbstractNavigableMapRateMeter},
 * which uses {@link ConcurrentSkipListMap} implementation of {@link NavigableMap} (this might be changed in the future).
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