package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import static stinc.male.sandbox.ratexecutor.Preconditions.checkArgument;
import static stinc.male.sandbox.ratexecutor.Preconditions.checkNotNull;

abstract class AbstractRateSampler implements RateSampler {
  private final StartNanos startNanos;
  private final Duration sampleInterval;
  private final long sampleIntervalNanos;

  /**
   * @param startNanos Starting point that is used to calculate elapsed nanoseconds.
   * @param sampleInterval Size of the sample window.
   * MUST NOT be {@linkplain Duration#isZero() zero} or {@linkplain Duration#isNegative() negative}.
   */
  AbstractRateSampler(final long startNanos, final Duration sampleInterval) {
    checkNotNull(sampleInterval, "sampleInterval");
    checkArgument(!sampleInterval.isZero(), "sampleInterval", "Must not be zero");
    checkArgument(!sampleInterval.isNegative(), "sampleInterval", "Must be positive");
    this.sampleInterval = sampleInterval;
    sampleIntervalNanos = sampleInterval.toNanos();
    this.startNanos = new StartNanos(startNanos, sampleIntervalNanos);
  }

  @Override
  public final long getStartNanos() {
    return startNanos.value;
  }

  @Override
  public final Duration getSampleInterval() {
    return sampleInterval;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "(startNanos=" + startNanos.value
        + ", sampleIntervalNanos=" + sampleIntervalNanos
        + ')';
  }

  /**
   * @return sampleInterval in nanoseconds.
   */
  protected final long getSampleIntervalNanos() {
    return sampleIntervalNanos;
  }

  /**
   * Checks if {@code nanos} is a valid tNanos value for the startNanos and sampleInterval given in the {@link AbstractRateSampler}'s constructor.
   *
   * @param nanos Value to check.
   * @param paramName Name of the method parameter with value {@code nanos} which will be used to generate error if {@code nanos} is not a valid value.
   * @throws IllegalArgumentException
   */
  protected final void checkTNanos(final long nanos, final String paramName) throws IllegalArgumentException {
    startNanos.check(nanos, paramName);
  }

  /**
   * Represents startNanos.
   */
  private static final class StartNanos {
    private final long value;
    private final long l1;
    private final long r1;
    private final long l2;
    private final long r2;

    StartNanos(final long value, final long sampleIntervalNanos) {
      this.value = value;
      if ((value - sampleIntervalNanos) <= 0) {
        l1 = value;
        r1 = value - sampleIntervalNanos + Long.MAX_VALUE;
        l2 = 0;
        r2 = 0;
      } else {
        l1 = value;
        r1 = Long.MAX_VALUE;
        l2 = Long.MIN_VALUE;
        r2 = Long.MIN_VALUE + value - sampleIntervalNanos - 1;
      }
    }

    /**
     * Checks if {@code nanos} is a valid tNanos value for the startNanos and sampleInterval given in the constructor of {@link StartNanos}.
     *
     * @throws IllegalArgumentException
     */
    final void check(final long nanos, final String paramName) throws IllegalArgumentException {
      if (l2 == 0) {//(value - sampleIntervalNanos) <= 0
        checkArgument(NanosComparator.compare(l1, nanos) <= 0 && NanosComparator.compare(nanos, r1) <= 0,
            paramName, () -> String.format("Must be in [%s; %s]", l1, r1));
      } else {
        checkArgument((NanosComparator.compare(l1, nanos) <= 0 && NanosComparator.compare(nanos, r1) <= 0)
                || (NanosComparator.compare(l2, nanos) <= 0 && NanosComparator.compare(nanos, r2) <= 0),
            paramName, () -> String.format("Must be in [%s; %s]\u222a[%s; %s]", l1, r1, l2, r2));
      }
    }
  }
}