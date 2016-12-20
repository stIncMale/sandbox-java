package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import java.util.TreeMap;
import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public class TreeMapRateMeter extends AbstractNavigableMapRateMeter<TreeMap<Long, TicksCounter>> {
  private static final RateMeterConfig defaultInstance = RateMeterConfig.newBuilder()
          .setTicksCounterSupplier(LongTicksCounter::new)
          .build();

  /**
   * @return A reasonable configuration.
   */
  public static final RateMeterConfig defaultConfig() {
    return defaultInstance;
  }

  /**
   * Constructor.
   *
   * @param startNanos Starting point that is used to calculate elapsed nanoseconds.
   * @param samplesInterval Size of the samples window.
   * @param config Additional configuration parameters.
   */
  public TreeMapRateMeter(final long startNanos, final Duration samplesInterval, final RateMeterConfig config) {
    super(startNanos, samplesInterval, config, () -> new TreeMap<>(NanosComparator.instance()));
  }

  /**
   * Acts like {@link #TreeMapRateMeter(long, Duration, RateMeterConfig)} with {@link #defaultConfig()}
   * as the third argument.
   */
  public TreeMapRateMeter(final long startNanos, final Duration samplesInterval) {
    this(startNanos, samplesInterval, defaultConfig());
  }
}