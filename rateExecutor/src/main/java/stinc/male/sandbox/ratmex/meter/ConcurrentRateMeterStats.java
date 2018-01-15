package stinc.male.sandbox.ratmex.meter;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Stats gathered by a thread-safe implementation of {@link RateMeter} which implements {@link RateMeter#tick(long, long)} in a relaxed way,
 * that allows come ticks to be registered at an incorrect instant for example.
 */
@ThreadSafe
public interface ConcurrentRateMeterStats {
  /**
   * @return The number of situations
   * when {@link RateMeter} might have {@linkplain RateMeter#tick(long, long) registered ticks} at an incorrect instant.
   */
  long incorrectlyRegisteredTicksEventsCount();
}