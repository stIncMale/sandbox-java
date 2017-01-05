package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import java.util.TreeMap;
import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public class NavigableMapRateMeter extends AbstractNavigableMapRateMeter<TreeMap<Long, TicksCounter>> {
  private static final RateMeterConfig defaultConfigInstance = RateMeterConfig.newBuilder()
          .setTicksCounterSupplier(LongTicksCounter::new)
          .build();

  /**
   * @return A reasonable configuration.
   */
  public static final RateMeterConfig defaultConfig() {
    return defaultConfigInstance;
  }

  /**
   * Constructor.
   *
   * @param startNanos Starting point that is used to calculate elapsed nanoseconds.
   * @param samplesInterval Size of the samples window.
   * @param config Additional configuration parameters.
   */
  public NavigableMapRateMeter(final long startNanos, final Duration samplesInterval, final RateMeterConfig config) {
    super(startNanos, samplesInterval, config, () -> new TreeMap<>(NanosComparator.instance()), true);
  }

  /**
   * Acts like {@link #NavigableMapRateMeter(long, Duration, RateMeterConfig)} with {@link #defaultConfig()}
   * as the third argument.
   */
  public NavigableMapRateMeter(final long startNanos, final Duration samplesInterval) {
    this(startNanos, samplesInterval, defaultConfig());
  }
}