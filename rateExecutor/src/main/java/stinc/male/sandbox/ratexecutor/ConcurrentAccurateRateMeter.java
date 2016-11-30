package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public final class ConcurrentAccurateRateMeter extends AbstractNavigableMapRateMeter {
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
  protected final void doGc() {
    if (aGcFlag.compareAndSet(false, true)) {
      try {
        final long rightNanos = rightSamplesWindowBoundary();
        final long leftNanos = rightNanos - getSamplesIntervalNanos();
        final NavigableMap<Long, TicksCounter> samples = getSamples();
        @Nullable
        final Long rightNanosToRemoveTo = samples.floorKey(leftNanos);
        if (rightNanosToRemoveTo != null) {
          samples.subMap(samples.firstKey(), true, rightNanosToRemoveTo, true)
              .clear();
        }
      } finally {
        aGcFlag.set(false);
      }
    }
  }
}