package stinc.male.sandbox.ratmex.executor;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import javax.annotation.concurrent.Immutable;
import stinc.male.sandbox.ratmex.util.internal.ConversionsAndChecks;
import static stinc.male.sandbox.ratmex.util.internal.ConversionsAndChecks.checkUnit;
import static stinc.male.sandbox.ratmex.util.internal.Preconditions.checkArgument;

/**
 * A representation of a rate.
 * Formally represents a set of probability distributions which have probability density functions equal to 0
 * for any values outside [{@linkplain #getMin() min}; {@linkplain #getMax() max}],
 * and {@linkplain #getMean() mean} equal to ({@linkplain #getMin() min} + {@linkplain #getMax() max}) / 2.
 */
@Immutable
public final class Rate {
  private final double min;
  private final double max;
  private final Duration unit;

  /**
   * @param min A {@linkplain #getMin() min} rate value.
   * @param max A {@linkplain #getMax() max} rate value.
   * @param unit A {@linkplain #getUnit() unit} in which the rate is measured.
   * {@link Duration} allows specifying not only such standard units as {@link ChronoUnit#SECONDS}, {@link ChronoUnit#HOURS},
   * but arbitrary time units. Must not be {@linkplain Duration#isZero() zero} or {@linkplain Duration#isNegative() negative}.
   */
  public Rate(final double min, final double max, final Duration unit) {
    checkArgument(min <= max, "max", () -> String.format("Must not be less than %s, but actual value is %s", min, max));
    checkUnit(unit, "unit");
    this.min = min;
    this.max = max;
    this.unit = unit;
  }

  /**
   * This method is equivalent to
   * {@link #withAbsoluteDeviation(double, double, Duration) withAbsoluteDeviation}{@code (mean, relativeDeviation * mean, unit)}.
   *
   * @param mean A {@linkplain #getMean() mean} rate value.
   * @param relativeDeviation A relative deviation of rate values from the {@code mean}. Must not be negative.
   * @param unit See {@link #Rate(double, double, Duration)}.
   */
  public static Rate withRelativeDeviation(final double mean, final double relativeDeviation, final Duration unit) {
    checkArgument(
        relativeDeviation >= 0,
        "relativeDeviation",
        () -> String.format("Must not be less than 0, but actual value is %s", relativeDeviation));
    return withAbsoluteDeviation(mean, relativeDeviation * mean, unit);
  }

  /**
   * This method is equivalent to
   * {@code new }{@link #Rate(double, double, Duration) Rate}{@code (mean - absoluteDeviation, mean + absoluteDeviation, unit)},
   * but may return not a new object.
   *
   * @param mean A {@linkplain #getMean() mean} rate value.
   * @param absoluteDeviation An absolute deviation of rate values from the {@code mean}.
   * @param unit See {@link #Rate(double, double, Duration)}.
   */
  public static Rate withAbsoluteDeviation(final double mean, final double absoluteDeviation, final Duration unit) {
    return new Rate(mean - absoluteDeviation, mean + absoluteDeviation, unit);
  }

  /**
   * @return A min rate value.
   */
  public final double getMin() {
    return min;
  }

  /**
   * @return A max rate value.
   */
  public final double getMax() {
    return max;
  }

  /**
   * @return A mean rate value which is equal to ({@linkplain #getMin() min} + {@linkplain #getMax() max}) / 2.
   */
  public final double getMean() {
    return (min + max) / 2;
  }

  /**
   * @return A {@linkplain #getUnit() unit} in which the rate is measured.
   */
  private final Duration getUnit() {
    return unit;
  }

  /**
   * Converts rate from representation in {@link #getUnit()} to representation in the specified {@code unit}s.
   *
   * @param unit See {@link #Rate(double, double, Duration)}.
   * @return A converted {@link Rate}, which may be not new.
   */
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

  /**
   * @return true if {@code obj} is a {@link Rate}
   * and both rates have equal {@linkplain #getMin() min}, {@linkplain #getMax() max}, {@linkplain #getUnit() unit}.
   * false otherwise.
   */
  @Override
  public final boolean equals(final Object obj) {
    final boolean result;
    if (this == obj) {
      result = true;
    } else if (obj instanceof Rate) {
      final Rate o = (Rate)obj;
      return min == o.min && max == o.max && unit.equals(o.unit);
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