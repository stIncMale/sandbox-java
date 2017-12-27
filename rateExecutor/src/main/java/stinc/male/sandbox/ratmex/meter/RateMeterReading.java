package stinc.male.sandbox.ratmex.meter;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public final class RateMeterReading {
    private long longValue;
    private double doubleValue;
    private long tNanos;
    private boolean accurate;
    private boolean rounded;

    public RateMeterReading() {
    }

    public final long getLongValue() {//TODO getValueLong
        return longValue;
    }

    public final double getDoubleValue() {//TODO getValueDouble
        return doubleValue;
    }

    public final long getTNanos() {
        return tNanos;
    }

    public final boolean isAccurate() {
        return accurate;
    }

    public final boolean isRounded() {
        return rounded;
    }

    public final RateMeterReading setValue(final long value) {
        longValue = value;
        doubleValue = value;
        rounded = false;
        return this;
    }

    public final RateMeterReading setValue(final long valueLong, final double valueDouble, final boolean rounded) {//TODO refactor this method?
        longValue = valueLong;
        doubleValue = valueDouble;
        this.rounded = true;
        return this;
    }

    public final RateMeterReading setTNanos(final long tNanos) {
        this.tNanos = tNanos;
        return this;
    }

    public final RateMeterReading setAccurate(final boolean accurate) {
        this.accurate = accurate;
        return this;
    }

    @Override
    public final String toString() {
        return getClass().getSimpleName()
                + "{longValue=" + longValue
                + ", doubleValue=" + doubleValue
                + ", tNanos=" + tNanos
                + ", accurate=" + accurate
                + ", rounded=" + rounded
                + '}';
    }
}