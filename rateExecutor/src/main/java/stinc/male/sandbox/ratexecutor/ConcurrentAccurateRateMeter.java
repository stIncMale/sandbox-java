package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class ConcurrentAccurateRateMeter extends AbstractNavigableMapRateMeter {
  private final AtomicBoolean aGcFlag;

  /**
   * Constructor.
   *
   * @param startNanos Starting point that is used to calculate elapsed nanoseconds.
   * @param samplesInterval Size of the samples window.
   * @param config Additional configuration parameters.
   */
  public ConcurrentAccurateRateMeter(final long startNanos, final Duration samplesInterval, final RateMeterConfig config) {
    super(startNanos, samplesInterval, config, () -> new ConcurrentSkipListMap<>(NanosComparator.getInstance()));
    aGcFlag = new AtomicBoolean();
  }

  /**
   * Acts like {@link #ConcurrentAccurateRateMeter(long, Duration, RateMeterConfig)} with {@link RateMeterConfig#defaultInstance()}
   * as the third argument.
   */
  public ConcurrentAccurateRateMeter(final long startNanos, final Duration samplesInterval) {
    this(startNanos, samplesInterval, RateMeterConfig.defaultInstance());
  }

  @Override
  protected void gc() {
    if (aGcFlag.compareAndSet(false, true)) {
      try {
        super.gc();
      } finally {
        aGcFlag.set(false);
      }
    }
  }
}