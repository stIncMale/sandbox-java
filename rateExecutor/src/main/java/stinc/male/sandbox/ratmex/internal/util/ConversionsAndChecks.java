package stinc.male.sandbox.ratmex.internal.util;

import java.time.Duration;
import javax.annotation.Nullable;
import stinc.male.sandbox.ratmex.meter.NanosComparator;
import stinc.male.sandbox.ratmex.meter.RateMeterReading;
import static stinc.male.sandbox.ratmex.internal.util.Preconditions.checkArgument;
import static stinc.male.sandbox.ratmex.internal.util.Preconditions.checkNotNull;

public final class ConversionsAndChecks {
  /**
   * Checks if {@code tNanos} is a valid value for the given {@code safeStartNanos} and {@code safeMaxTNanos}.
   *
   * @param tNanos Value to check.
   * @param safeParamName Name of the method parameter with value {@code tNanos}
   * which will be used to generate error if {@code tNanos} is not a valid value.
   *
   * @throws IllegalArgumentException If {@code tNanos} is invalid.
   */
  public static final void checkTNanos(
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
          String.format("The argument %s=%s is illegal. Must be in [%s; %s] (comparison according to System.nanoTime())",
              safeParamName, tNanos, safeStartNanos, safeMaxTNanos), cause);
    }
  }

  public static final void checkUnit(final Duration unit, final String safeParamName) throws IllegalArgumentException {
    checkNotNull(unit, safeParamName);
    checkArgument(!unit.isZero(), safeParamName, "Must not be zero");
    checkArgument(!unit.isNegative(), safeParamName, "Must not be negative");
  }

  public static final void checkDuration(final Duration duration, final String safeParamName) throws IllegalArgumentException {
    checkNotNull(duration, safeParamName);
    checkArgument(!duration.isNegative(), safeParamName, "Must not be negative");
  }

  public static final long maxTNanos(final long startNanos, final long safeSamplesIntervalNanos, final long safeHistoryLength) {
    return startNanos - safeHistoryLength * safeSamplesIntervalNanos + Long.MAX_VALUE;
  }

  public static final double rateAverage(
      final long safeRightNanos,
      final long safeUnitSizeNanos,
      final long safeStartNanos,
      final long safeTicksTotalCount) {
    final long totalNanos = safeRightNanos - safeStartNanos;
    return totalNanos == 0
        ? 0
        : (double)safeTicksTotalCount / ((double)totalNanos / safeUnitSizeNanos);
  }

  public static final double convertRate(
      final double rateInUnits,
      final long safeUnitSizeNanos,
      final long newSafeUnitSizeNanos) {
    return (newSafeUnitSizeNanos == safeUnitSizeNanos)
        ? rateInUnits
        : rateInUnits / ((double)safeUnitSizeNanos / newSafeUnitSizeNanos);
  }

  public static final RateMeterReading convertRate(
      final RateMeterReading rateReadingInUnits,
      final long safeUnitSizeNanos,
      final long newSafeUnitSizeNanos) {
    final RateMeterReading result;
    if (safeUnitSizeNanos == newSafeUnitSizeNanos) {
      result = rateReadingInUnits;
    } else {
      final double convertedRate = convertRate(rateReadingInUnits.getValueDouble(), safeUnitSizeNanos, newSafeUnitSizeNanos);
      return rateReadingInUnits.setValue(convertedRate);
    }
    return result;
  }

  private ConversionsAndChecks() {
    throw new UnsupportedOperationException("The class isn't designed to be instantiated");
  }
}