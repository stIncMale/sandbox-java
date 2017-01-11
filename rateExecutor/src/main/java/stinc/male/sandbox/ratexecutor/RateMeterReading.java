package stinc.male.sandbox.ratexecutor;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public final class RateMeterReading {
    private long longValue;
    private double doubleValue;
    private long tNanos;
    private boolean accurate;

    public RateMeterReading() {
    }

    public final double getDoubleValue() {
        return doubleValue;
    }

    public final long getLongValue() {
        return longValue;
    }

    public final long getTNanos() {
        return tNanos;
    }

    public final boolean isAccurate() {
        return accurate;
    }

    final RateMeterReading setValue(final double value) {
        longValue = (long)value;
        doubleValue = value;
        return this;
    }

    final RateMeterReading setValue(final long value) {
        longValue = value;
        doubleValue = value;
        return this;
    }

    final RateMeterReading setTNanos(final long tNanos) {
        this.tNanos = tNanos;
        return this;
    }

    final RateMeterReading setAccurate(final boolean accurate) {
        this.accurate = accurate;
        return this;
    }

    @Override
    public final String toString() {
        return getClass().getSimpleName()
                + "(longValue=" + longValue
                + ", doubleValue=" + doubleValue
                + ", tNanos=" + tNanos
                + ", accurate=" + accurate
                + ')';
    }
}