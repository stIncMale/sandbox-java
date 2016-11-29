package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import javax.annotation.Nullable;
import static stinc.male.sandbox.ratexecutor.Preconditions.checkArgument;
import static stinc.male.sandbox.ratexecutor.Preconditions.checkNotNull;

abstract class AbstractRateMeter implements RateMeter {
  private final long startNanos;
  private final Duration samplesInterval;
  private final long samplesIntervalNanos;
  private final long maxTNanos;
  private final boolean checkTNanos;

  /**
   * @param startNanos Starting point that is used to calculate elapsed nanoseconds.
   * @param samplesInterval Size of the samples window.
   * @param config Additional configuration parameters.
   */
  AbstractRateMeter(final long startNanos, final Duration samplesInterval, final RateMeterConfig config) {
    checkNotNull(samplesInterval, "samplesInterval");
    checkArgument(!samplesInterval.isZero(), "samplesInterval", "Must not be zero");
    checkArgument(!samplesInterval.isNegative(), "samplesInterval", "Must be positive");
    checkNotNull(config, "config");
    this.startNanos = startNanos;
    this.samplesInterval = samplesInterval;
    samplesIntervalNanos = samplesInterval.toNanos();
    checkArgument(samplesIntervalNanos <= Long.MAX_VALUE - 1, "samplesInterval",
        () -> String.format("Must be less than (Long.MAX_VALUE - 1)nanos = %snanos, but actual value is %s", Long.MAX_VALUE - 1, samplesIntervalNanos));
    maxTNanos = startNanos - samplesIntervalNanos + Long.MAX_VALUE;
    this.checkTNanos = config.isCheckTNanos();
  }

  @Override
  public final long getStartNanos() {
    return startNanos;
  }

  @Override
  public final Duration getSamplesInterval() {
    return samplesInterval;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "(startNanos=" + startNanos
        + ", samplesIntervalNanos=" + samplesIntervalNanos
        + ')';
  }

  /**
   * @return samplesInterval in nanoseconds.
   */
  protected final long getSamplesIntervalNanos() {
    return samplesIntervalNanos;
  }

  /**
   * Checks if {@code tNanos} is a valid value for the startNanos and samplesInterval,
   * or does nothing if {@link RateMeterConfig#isCheckTNanos()} is {@code false}.
   *
   * @param tNanos Value to check.
   * @param paramName Name of the method parameter with value {@code tNanos} which will be used to generate error if {@code tNanos} is not a valid value.
   * @throws IllegalArgumentException If {@code tNanos} is invalid.
   */
  protected final void checkTNanos(final long tNanos, final String paramName) throws IllegalArgumentException {
    if (checkTNanos) {
      boolean ok = false;
      @Nullable
      IllegalArgumentException cause = null;
      try {
        ok = NanosComparator.compare(startNanos, tNanos) <= 0 && NanosComparator.compare(tNanos, maxTNanos) <= 0;
      } catch (final IllegalArgumentException e) {
        cause = e;
      }
      if (!ok) {
        throw new IllegalArgumentException(
            String.format("Must be in [%s; %s] (comparison according to System.nanoTime()), but actual value is %s",
                startNanos, maxTNanos, tNanos), cause);
      }
    }
  }
}