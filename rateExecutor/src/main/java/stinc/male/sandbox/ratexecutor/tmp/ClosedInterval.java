package stinc.male.sandbox.ratexecutor.tmp;

import javax.annotation.concurrent.Immutable;
import static stinc.male.sandbox.ratexecutor.Preconditions.checkArgument;

@Immutable
public final class ClosedInterval {
  private final long min;
  private final long max;
  private final double mean;

  public ClosedInterval(final long min, final long max) {
    checkArgument(min <= max, "max", () -> String.format("Must not be less than %s, but actual value is %s", min, max));
    this.min = min;
    this.max = max;
    mean = (double)Math.addExact(max, min) / 2d;
  }

  public static ClosedInterval of(final long mean, final double relativeDeviation) {
    checkArgument(
        relativeDeviation >= 0,
        "relativeDeviation",
        () -> String.format("Must not be less than 0, but actual value is %s", relativeDeviation));
    checkArgument(
        relativeDeviation <= 1,
        "relativeDeviation",
        () -> String.format("Must not be greater than 1, but actual value is %s", relativeDeviation));
    final long absoluteDeviation = Math.round(relativeDeviation * mean);
    return of(mean, absoluteDeviation);
  }

  public static ClosedInterval of(final long mean, final long absoluteDeviation) {
    return new ClosedInterval(Math.subtractExact(mean, absoluteDeviation), Math.addExact(mean, absoluteDeviation));
  }

  public final long getMin() {
    return min;
  }

  public final long getMax() {
    return max;
  }

  public final double getMean() {
    return mean;
  }

  public final boolean isWithin(final long v) {
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
    return (int)(min + max);
  }

  @Override
  public final String toString() {
    return "[" + min + "; " + max + "]";
  }
}