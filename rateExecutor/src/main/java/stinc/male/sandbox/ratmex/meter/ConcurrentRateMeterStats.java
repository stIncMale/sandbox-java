package stinc.male.sandbox.ratmex.meter;

import java.util.concurrent.atomic.LongAdder;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
final class ConcurrentRateMeterStats implements RateMeterStats {
  private final LongAdder failedAccuracyEventsCountForTick;
  private final LongAdder failedAccuracyEventsCountForRate;

  ConcurrentRateMeterStats() {
    failedAccuracyEventsCountForTick = new LongAdder();
    failedAccuracyEventsCountForRate = new LongAdder();
  }

  @Override
  public final long failedAccuracyEventsCountForTick() {
    return failedAccuracyEventsCountForTick.sum();
  }

  @Override
  public final long failedAccuracyEventsCountForRate() {
    return failedAccuracyEventsCountForRate.sum();
  }

  public final void accountFailedAccuracyEventForTick() {
    failedAccuracyEventsCountForTick.increment();
  }

  public final void accountFailedAccuracyEventForRate() {
    failedAccuracyEventsCountForRate.increment();
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName() +
            "{failedAccuracyEventsCountForTick=" + failedAccuracyEventsCountForTick +
            ", failedAccuracyEventsCountForRate=" + failedAccuracyEventsCountForRate +
            '}';
  }
}