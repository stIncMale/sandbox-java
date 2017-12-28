package stinc.male.sandbox.ratmex.meter;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * A modifiable {@link RateMeter} reading.
 */
@NotThreadSafe
public final class RateMeterReading {
  private long valueLong;
  private double valueDouble;
  private long tNanos;
  private boolean accurate;
  private boolean rounded;

  public RateMeterReading() {
  }

  /**
   * @return long representation of the value read. Either {@linkplain #isRounded() rounded}, or not.
   * Note that even if the reading is not {@linkplain #isRounded() rounded}, this value may not be equal to {@link #getValueDouble()}
   * because not every long value can be exactly represented as double value.
   */
  public final long getValueLong() {
    return valueLong;
  }

  /**
   * @return double representation of the value read.
   * Note that even if the reading is not {@linkplain #isRounded() rounded}, this value may not be equal to {@link #getValueLong()}
   * because not every long value can be exactly represented as double value.
   */
  public final double getValueDouble() {
    return valueDouble;
  }

  /**
   * @return An instant (a pair (startNanos, elapsedNanos), see {@link RateMeter}) at which the reading was taken.
   * The more specific semantics may depend on the semantics of the {@link RateMeter}'s method via which the reading was made
   * and/or on the implementation details.
   */
  public final long getTNanos() {
    return tNanos;
  }

  /**
   * @return A flag that specifies whether a {@link RateMeter} was able to provide an accurate reading,
   * or it failed and returned average or approximate rate (see {@link RateMeter}).
   */
  public final boolean isAccurate() {
    return accurate;
  }

  /**
   * @return A flag that specifies whether {@link #getValueLong()} returns a {@linkplain Math#round(double) rounded} representation of
   * {@link #getValueDouble()}, or not.
   */
  public final boolean isRounded() {
    return rounded;
  }

  /**
   * Sets both {@linkplain #getValueLong() long} and {@linkplain #getValueDouble() double} values.
   * Marks the reading as not {@linkplain #isRounded() rounded}.
   *
   * @param value A long value.
   *
   * @return {@code this}.
   */
  public final RateMeterReading setValue(final long value) {
    valueLong = value;
    valueDouble = value;
    rounded = false;
    return this;
  }

  /**
   * Sets both {@linkplain #getValueDouble() double} and {@linkplain #getValueLong() long} values.
   * Marks the reading as {@linkplain #isRounded() rounded}.
   *
   * @param value A long value.
   *
   * @return {@code this}.
   */
  public final RateMeterReading setValue(final double value) {
    valueLong = Math.round(value);
    valueDouble = value;
    rounded = true;
    return this;
  }

  /**
   * @param accurate See {@link #isAccurate()}.
   *
   * @return {@code this}.
   */
  public final RateMeterReading setAccurate(final boolean accurate) {
    this.accurate = accurate;
    return this;
  }

  /**
   * @param tNanos See {@link #getTNanos()}.
   *
   * @return {@code this}.
   */
  public final RateMeterReading setTNanos(final long tNanos) {
    this.tNanos = tNanos;
    return this;
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName()
        + "{valueLong=" + valueLong
        + ", valueDouble=" + valueDouble
        + ", tNanos=" + tNanos
        + ", accurate=" + accurate
        + ", rounded=" + rounded
        + '}';
  }
}