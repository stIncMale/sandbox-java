package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import javax.annotation.Nullable;
import static stinc.male.sandbox.ratexecutor.Preconditions.checkArgument;
import static stinc.male.sandbox.ratexecutor.Preconditions.checkNotNull;

/**
 * This class is only required because in Java 8 one can not define a private or package private method,
 * so it is not possible to define this methods as package private in {@link RateMeter}.
 */
final class RateMeterMath {
  /**
   * Checks if {@code tNanos} is a valid value for the given {@code safeStartNanos} and {@code safeMaxTNanos}.
   *
   * @param tNanos Value to check.
   * @param safeParamName Name of the method parameter with value {@code tNanos} which will be used to generate error if {@code tNanos} is not a valid value.
   * @throws IllegalArgumentException If {@code tNanos} is invalid.
   */
  static final void checkTNanos(
      final long tNanos,
      final long safeStartNanos,
      final long safeMaxTNanos,
      final String safeParamName) throws IllegalArgumentException {
    boolean ok = false;
    @Nullable
    IllegalArgumentException cause = null;
    try {
      ok = NanosComparator.compare(safeStartNanos, tNanos) <= 0 && NanosComparator.compare(tNanos, safeMaxTNanos) <= 0;
    } catch (final IllegalArgumentException e) {
      cause = e;
    }
    if (!ok) {
      throw new IllegalArgumentException(
          String.format("The argument %s is illegal. Must be in [%s; %s] (comparison according to System.nanoTime()), but actual value is %s",
              safeParamName, safeStartNanos, safeMaxTNanos, tNanos), cause);
    }
  }

  static final void checkUnit(final Duration unit, final String safeParamName) throws IllegalArgumentException {
    checkNotNull(unit, safeParamName);
    checkArgument(!unit.isZero(), safeParamName, "Must not be zero");
    checkArgument(!unit.isNegative(), safeParamName, "Must be positive");
  }

  static final long maxTNanos(final long startNanos, final long safeSamplesIntervalNanos) {
    return startNanos - safeSamplesIntervalNanos + Long.MAX_VALUE;
  }

  static final double rateAverage(
      final long safeTNanos,
      final long safeUnitSizeNanos,
      final long safeStartNanos,
      final long safeTicksTotalCount) {
    final long totalNanos = safeTNanos - safeStartNanos;
    return totalNanos == 0
        ? 0
        : (double)safeTicksTotalCount / ((double)totalNanos / safeUnitSizeNanos);
  }

  static final double convertRate(
      final double rateInSamplesIntervals,
      final long safeSamplesIntervalNanos,
      final long safeUnitSizeNanos) {
    return (safeUnitSizeNanos == safeSamplesIntervalNanos)
        ? rateInSamplesIntervals
        : rateInSamplesIntervals / ((double) safeSamplesIntervalNanos / safeUnitSizeNanos);
  }

  private RateMeterMath() {
    throw new UnsupportedOperationException("The class isn't designed to be instantiated");
  }
}