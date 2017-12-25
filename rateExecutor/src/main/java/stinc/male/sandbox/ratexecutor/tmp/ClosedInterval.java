package stinc.male.sandbox.ratexecutor.tmp;

import javax.annotation.concurrent.Immutable;
import static stinc.male.sandbox.ratexecutor.Preconditions.checkArgument;

@Immutable
public final class ClosedInterval {
  private final double min;
  private final double max;
  private final double mean;

  public ClosedInterval(final double min, final double max) {
    checkArgument(min <= max, "max", () -> String.format("Must not be less than %s, but actual value is %s", min, max));
    this.min = min;
    this.max = max;
    mean = (min + max) / 2;
  }

  public static ClosedInterval withRelativeDeviation(final double mean, final double relativeDeviation) {
    checkArgument(
        relativeDeviation >= 0,
        "relativeDeviation",
        () -> String.format("Must not be less than 0, but actual value is %s", relativeDeviation));
    checkArgument(
        relativeDeviation <= 1,
        "relativeDeviation",
        () -> String.format("Must not be greater than 1, but actual value is %s", relativeDeviation));
    return withAbsoluteDeviation(mean, relativeDeviation * mean);
  }

  public static ClosedInterval withAbsoluteDeviation(final double mean, final double absoluteDeviation) {
    return new ClosedInterval(mean - absoluteDeviation, mean + absoluteDeviation);
  }

  public final double getMin() {
    return min;
  }

  public final double getMax() {
    return max;
  }

  public final double getMean() {
    return mean;
  }

  public final boolean isWithin(final double v) {
    return v >= min && v <= max;
  }

  @Override
  public final boolean equals(final Object o) {
    final boolean result;
    if (this == o) {
      result = true;
    } else if (o instanceof ClosedInterval) {
      final ClosedInterval obj = (ClosedInterval)o;
      return min == obj.min && max == obj.max;
    } else {
      result = false;
    }
    return result;
  }

  @Override
  public final int hashCode() {
    return (int)(min + 31 * max);
  }

  @Override
  public final String toString() {
    return "[" + min + "; " + max + "]";
  }
}