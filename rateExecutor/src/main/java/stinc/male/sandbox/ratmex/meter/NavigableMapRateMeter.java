package stinc.male.sandbox.ratmex.meter;

import java.time.Duration;
import java.util.NavigableMap;
import java.util.TreeMap;
import javax.annotation.concurrent.NotThreadSafe;
import stinc.male.sandbox.ratmex.NanosComparator;

/**
 * A not thread-safe implementation of {@link AbstractNavigableMapRateMeter},
 * which currently uses {@link TreeMap} implementation of {@link NavigableMap} (this might be changed in the future).
 */
@NotThreadSafe
public final class NavigableMapRateMeter extends AbstractNavigableMapRateMeter<ConcurrentRateMeterConfig> {
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
  public NavigableMapRateMeter(final long startNanos, final Duration samplesInterval, final RateMeterConfig config) {
    this(startNanos, samplesInterval, ((ConcurrentRateMeterConfig.Builder)defaultConfig().toBuilder()
        .set(config))
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