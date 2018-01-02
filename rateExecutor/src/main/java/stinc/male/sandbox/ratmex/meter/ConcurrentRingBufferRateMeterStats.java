package stinc.male.sandbox.ratmex.meter;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Stats gathered by {@link ConcurrentRingBufferRateMeter}.
 */
@ThreadSafe
public interface ConcurrentRingBufferRateMeterStats {
  /**
   * @return The number of situations when {@link ConcurrentRingBufferRateMeter} might have registered ticks at an incorrect instant.
   * This is only relevant if
   * {@link AbstractRateMeter#getConfig() getConfig()}{@code .}{@link ConcurrentRingBufferRateMeterConfig#isStrictTick() isStrictTick()} is false.
   */
  long failedAccuracyEventsCountForTick();
}