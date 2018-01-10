package stinc.male.sandbox.ratmex.executor;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import javax.annotation.concurrent.Immutable;
import stinc.male.sandbox.ratmex.meter.RateMeterReading;
import static stinc.male.sandbox.ratmex.internal.util.ConversionsAndChecks.checkUnit;
import static stinc.male.sandbox.ratmex.internal.util.Preconditions.checkArgument;
import static stinc.male.sandbox.ratmex.internal.util.Preconditions.checkNotNull;
import static stinc.male.sandbox.ratmex.internal.util.Util.format;

/**
 * A representation of a rate.
 * Formally represents a set of probability distributions which have probability density functions equal to 0
 * for any rate values outside [{@linkplain #getMinValue() minValue}; {@linkplain #getMaxValue() max}].
 */
@Immutable
public final class Rate {
  private final double minValue;
  private final double maxValue;
  private final Duration unit;

  /**
   * @param minValue A {@linkplain #getMinValue() minValue} rate value.
   * @param maxValue A {@linkplain #getMaxValue() maxValue} rate value.
   * @param unit A {@linkplain #getUnit() unit} in which the rate is measured.
   * {@link Duration} allows specifying not only such standard units as {@link ChronoUnit#SECONDS}, {@link ChronoUnit#HOURS},
   * but arbitrary time units. Must not be {@linkplain Duration#isZero() zero} or {@linkplain Duration#isNegative() negative}.
   */
  public Rate(final double minValue, final double maxValue, final Duration unit) {
    checkArgument(minValue <= maxValue, "maxValue",
        () -> format("Must not be less than %s=%s, but actual value is %s", "minValue", minValue, maxValue));
    checkUnit(unit, "unit");
    this.minValue = minValue;
    this.maxValue = maxValue;
    this.unit = unit;
  }

  /**
   * This method is equivalent to
   * {@link #withAbsoluteDeviation(double, double, Duration) withAbsoluteDeviation}{@code (averageValue, relativeDeviation * averageValue, unit)}.
   *
   * @param averageValue An unweighted averageValue rate value.
   * @param relativeDeviation A maximal relative deviation of rate values from the {@code averageValue}. Must not be negative.
   * @param unit See {@link #Rate(double, double, Duration)}.
   */
  public static Rate withRelativeDeviation(final double averageValue, final double relativeDeviation, final Duration unit) {
    checkArgument(relativeDeviation >= 0, "relativeDeviation",
        () -> format("Must not be less than 0, but actual value is %s", relativeDeviation));
    checkUnit(unit, "unit");
    return withAbsoluteDeviation(averageValue, relativeDeviation * averageValue, unit);
  }

  /**
   * This method is equivalent to
   * {@code new }{@link #Rate(double, double, Duration) Rate}{@code (averageValue - absoluteDeviation, averageValue + absoluteDeviation, unit)},
   * but may return not a new object.
   *
   * @param averageValue An unweighted averageValue rate value.
   * @param absoluteDeviation An maximal absolute deviation of rate values from the {@code averageValue}. Must not be negative.
   * @param unit See {@link #Rate(double, double, Duration)}.
   */
  public static Rate withAbsoluteDeviation(final double averageValue, final double absoluteDeviation, final Duration unit) {
    checkArgument(absoluteDeviation >= 0, "absoluteDeviation",
        () -> format("Must not be negative, but actual value is %s", absoluteDeviation));
    checkUnit(unit, "unit");
    return new Rate(averageValue - absoluteDeviation, averageValue + absoluteDeviation, unit);
  }

  /**
   * @return A minValue rate value.
   */
  public final double getMinValue() {
    return minValue;
  }

  /**
   * @return A maxValue rate value.
   */
  public final double getMaxValue() {
    return maxValue;
  }

  /**
   * @return A {@linkplain #getUnit() unit} in which the rate is measured.
   */
  public final Duration getUnit() {
    return unit;
  }

  /**
   * Converts rate from representation in {@link #getUnit()} to representation in the specified {@code unit}s.
   *
   * @param unit See {@link #Rate(double, double, Duration)}.
   *
   * @return A converted {@link Rate}, which may be not new.
   */
  public final Rate toUnit(final Duration unit) {
    checkUnit(unit, "unit");
    final Rate result;
    if (this.unit.equals(unit)) {
      result = this;
    } else {
      final double ratio = (double)unit.toNanos() / this.unit.toNanos();
      result = new Rate(minValue * ratio, maxValue * ratio, unit);
    }
    return result;
  }

  /**
   * Decides if the provided rate (represented by {@code value} and {@code unit})
   * lies within, on the left of or on the right of the [minValue, maxValue] interval
   * (accounting that the interval may be expressed in different {@linkplain #getUnit() units}).
   *
   * @param value A value of the rate.
   * @param unit A time interval in which {@code value} is measured, i.e. rate is measured in unit<sup>-1</sup>.
   *
   * @return <ul>
   * <li>0 if the provided rate lies within the interval expressed by this {@link Rate};</li>
   * <li>1 if the provided rate lies on the left of the interval expressed by this {@link Rate};</li>
   * <li>-1 if the provided rate lies on the left of the interval expressed by this {@link Rate}.</li>
   * </ul>
   * So in a way these values are consistent with the logic of {@link Comparable#compareTo(Object)}.
   */
  public final int compareTo(final double value, final Duration unit) {
    checkUnit(unit, "unit");
    final int result;
    final double scaledValue;
    if (this.unit.equals(unit)) {//Duration.compareTo is consistent with Duration.equals, so we can use them interchangeably
      scaledValue = value;
    } else {
      final double ratio = this.unit.toNanos() / (double)unit.toNanos();
      scaledValue = value * ratio;
    }
    if (maxValue < scaledValue) {
      result = -1;
    } else if (scaledValue < minValue) {
      result = 1;
    } else {
      result = 0;
    }
    return result;
  }

  /**
   * @param reading Must not be null.
   *
   * @return {@link #compareTo(RateMeterReading) compareTo}{@code (}
   * {@link RateMeterReading#getValueDouble() reading.getValueDouble()}{@code ,}{@link RateMeterReading#getUnit() reading.getUnit()}{@code )}.
   */
  public final int compareTo(final RateMeterReading reading) {
    checkNotNull(reading, "reading");
    return compareTo(reading.getValueDouble(), reading.getUnit());
  }

  /**
   * @return true if {@code obj} is a {@link Rate}
   * and both rates have equal {@linkplain #getMinValue() min}, {@linkplain #getMaxValue() max}, {@linkplain #getUnit() unit}.
   * false otherwise.
   */
  @Override
  public final boolean equals(final Object obj) {
    final boolean result;
    if (this == obj) {
      result = true;
    } else if (obj instanceof Rate) {
      final Rate o = (Rate)obj;
      return minValue == o.minValue && maxValue == o.maxValue && unit.equals(o.unit);
    } else {
      result = false;
    }
    return result;
  }

  @Override
  public final int hashCode() {
    return 31 * (31 * (31 + (int)minValue) + (int)maxValue) + unit.hashCode();
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName() +
        "{minValue=" + minValue +
        ", maxValue=" + maxValue +
        ", unit=" + unit +
        '}';
  }
}