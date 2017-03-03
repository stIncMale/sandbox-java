package stinc.male.sandbox.ratexecutor;

import javax.annotation.concurrent.Immutable;
import static stinc.male.sandbox.ratexecutor.Preconditions.checkArgument;

@Immutable
public final class ClosedInterval {
    private final long left;
    private final long right;

    public ClosedInterval(final long left, final long right) {
        checkArgument(left <= right, "right", () -> String.format("Must not be less than %s, but actual value is %s", left, right));
        this.left = left;
        this.right = right;
    }

    public static ClosedInterval of(final long mean, final double relativeDeviation) {
        checkArgument(relativeDeviation >= 0, "relativeDeviation", () -> String.format("Must not be less than 0, but actual value is %s", relativeDeviation));
        checkArgument(relativeDeviation <= 1, "relativeDeviation", () -> String.format("Must not be greater than 1, but actual value is %s", relativeDeviation));
        final long absoluteDeviation = Math.round(relativeDeviation * mean);
        return of(mean, absoluteDeviation);
    }

    public static ClosedInterval of(final long mean, final long absoluteDeviation) {
        return new ClosedInterval(Math.subtractExact(mean, absoluteDeviation), Math.addExact(mean, absoluteDeviation));
    }

    public final long getLeft() {
        return left;
    }

    public final long getRight() {
        return right;
    }

    public final boolean isWithin(final long v) {
        return v >= left && v <= right;
    }

    @Override
    public final boolean equals(final Object o) {
        final boolean result;
        if (this == o) {
            result = true;
        } else if (o instanceof ClosedInterval) {
            final ClosedInterval obj = (ClosedInterval)o;
            return left == obj.left && right == obj.right;
        } else {
            result = false;
        }
        return result;
    }

    @Override
    public final int hashCode() {
        return (int)(left + right);
    }

    @Override
    public final String toString() {
        return '[' + left + "; " + right + ']';
    }
}