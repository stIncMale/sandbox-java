package stinc.male.sandbox.ratmex.meter;

import java.util.concurrent.atomic.LongAdder;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
final class ConcurrentRingBufferRateMeterStats implements ConcurrentRateMeterStats {
  private final LongAdder incorrectlyRegisteredTicksEventsCounter;

  ConcurrentRingBufferRateMeterStats() {
    incorrectlyRegisteredTicksEventsCounter = new LongAdder();
  }

  @Override
  public final long incorrectlyRegisteredTicksEventsCount() {
    return incorrectlyRegisteredTicksEventsCounter.sum();
  }

  public final void registerIncorrectlyRegisteredTicksEvent() {
    incorrectlyRegisteredTicksEventsCounter.increment();
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName() +
        "{incorrectlyRegisteredTicksEventsCounter=" + incorrectlyRegisteredTicksEventsCounter +
        '}';
  }
}