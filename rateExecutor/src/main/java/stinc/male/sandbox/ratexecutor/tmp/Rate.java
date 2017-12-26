package stinc.male.sandbox.ratexecutor.tmp;

import java.time.Duration;
import javax.annotation.concurrent.Immutable;
import stinc.male.sandbox.ratexecutor.util.internal.ConversionsAndChecks;
import static stinc.male.sandbox.ratexecutor.util.internal.ConversionsAndChecks.checkUnit;
import static stinc.male.sandbox.ratexecutor.util.internal.Preconditions.checkArgument;

/**
 * A representation of a rate.
 */
@Immutable
public final class Rate {
  private final double min;
  private final double max;
  private final Duration unit;

  public Rate(final double min, final double max, final Duration unit) {
    checkArgument(min <= max, "max", () -> String.format("Must not be less than %s, but actual value is %s", min, max));
    checkUnit(unit, "unit");
    this.min = min;
    this.max = max;
    this.unit = unit;
  }

  public static Rate withRelativeDeviation(final double mean, final double relativeDeviation, final Duration unit) {
    checkArgument(
        relativeDeviation >= 0,
        "relativeDeviation",
        () -> String.format("Must not be less than 0, but actual value is %s", relativeDeviation));
    checkArgument(
        relativeDeviation <= 1,
        "relativeDeviation",
        () -> String.format("Must not be greater than 1, but actual value is %s", relativeDeviation));
    return withAbsoluteDeviation(mean, relativeDeviation * mean, unit);
  }

  public static Rate withAbsoluteDeviation(final double mean, final double absoluteDeviation, final Duration unit) {
    return new Rate(mean - absoluteDeviation, mean + absoluteDeviation, unit);
  }

  public final double getMin() {
    return min;
  }

  public final double getMax() {
    return max;
  }

  public final double getMean() {
    return (min + max) / 2;
  }

  private final Duration getUnit() {
    return unit;
  }

  public final Rate toUnit(final Duration unit) {
    checkUnit(unit, "unit");
    final Rate result;
    if (this.unit.equals(unit)) {
      result = this;
    } else {
      final long fromUnitNanos = this.unit.toNanos();
      final long toUnitNanos = unit.toNanos();
      final double min = ConversionsAndChecks.convertRate(this.min, fromUnitNanos, toUnitNanos);
      final double max = ConversionsAndChecks.convertRate(this.max, fromUnitNanos, toUnitNanos);
      result = new Rate(min, max, unit);
    }
    return result;
  }

  @Override
  public final boolean equals(final Object o) {
    final boolean result;
    if (this == o) {
      result = true;
    } else if (o instanceof Rate) {
      final Rate obj = (Rate)o;
      return min == obj.min && max == obj.max;
    } else {
      result = false;
    }
    return result;
  }

  @Override
  public final int hashCode() {
    return 31 * (31 * (31 + (int)min) + (int)max) + unit.hashCode();
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName() +
        "{min=" + min +
        ", max=" + max +
        ", unit=" + unit +
        '}';
  }
}