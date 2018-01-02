package stinc.male.sandbox.ratmex.meter;

import java.util.concurrent.atomic.LongAdder;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
final class DefaultConcurrentRingBufferRateMeterStats implements ConcurrentRingBufferRateMeterStats {
  private final LongAdder failedAccuracyEventsCountForTick;

  DefaultConcurrentRingBufferRateMeterStats() {
    failedAccuracyEventsCountForTick = new LongAdder();
  }

  @Override
  public final long failedAccuracyEventsCountForTick() {
    return failedAccuracyEventsCountForTick.sum();
  }

  public final void registerFailedAccuracyEventForTick() {
    failedAccuracyEventsCountForTick.increment();
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName() +
        "{failedAccuracyEventsCountForTick=" + failedAccuracyEventsCountForTick +
        '}';
  }
}