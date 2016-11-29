package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import static stinc.male.sandbox.ratexecutor.Preconditions.checkArgument;
import static stinc.male.sandbox.ratexecutor.Preconditions.checkNotNull;

/**
 * A utility that measures rate of {@linkplain #tick(long, long) ticks}.
 * <p>
 * <b>Glossary</b><br>
 * <i>Instant</i><br>
 * {@link RateMeter} treats instants as the number of nanoseconds elapsed since the {@linkplain #getStartNanos() start}.
 * So instant is a pair (startNanos, elapsedNanos), but because startNanos is known and fixed,
 * we can equivalently specify an instant via a single value tNanos = startNanos + elapsedNanos.
 * {@link RateMeter} uses tNanos notation instead of (startNanos, elapsedNanos) notation.
 * All nanosecond values are compared as specified by {@link System#nanoTime()}.
 * <p>
 * <i>Samples window</i><br>
 * Samples window is a half-closed time interval
 * ({@linkplain #rightSamplesWindowBoundary() rightmostScoredInstant} - {@linkplain #getSamplesInterval() samplesInterval}; rightmostScoredInstant]
 * (comparison according to {@link System#nanoTime()}).
 * Current ticks are those inside the samples window.
 * Current rate is calculated base on current ticks only and by default is measured in samplesInterval<sup>-1</sup>.
 * <p>
 * For example if samplesInterval is 3s,<br>
 * startNanos is 1_000_000_000 ns,<br>
 * and the only scored ticks are<br>
 * 1 at 2_500_000_000 ns (this is tNanos, not tNanos - startNanos),<br>
 * 1 at 3_000_000_000 ns,<br>
 * 8 at 5_000_000_000 ns,<br>
 * -2 at 6_000_000_000 ns,<br>
 * then the current rate is<br>
 * (8 - 2) / samplesInterval = 6samplesInterval<sup>-1</sup> = 2s<sup>-1</sup> because samplesInterval is 3s.
 * <pre>
 *             2_500_000_000 ns         5_000_000_000 ns
 *                    |                        |                                                 t
 * ----|---------|----1----1---------|---------8--------(-2)------|---------|---------|---------&gt;
 *     |                   |                             |
 * startNanos       3_000_000_000 ns              6_000_000_000 ns
 *                         (--------samples window-------]
 * </pre>
 * <p>
 * <b>Allowed values</b><br>
 * samplesInterval \u2208 [1, {@link Long#MAX_VALUE} - 1],<br>
 * tNanos \u2208 [startNanos, startNanos - samplesInterval + {@link Long#MAX_VALUE}]
 * (comparison according to {@link System#nanoTime()}).
 */
public interface RateMeter {
  /**
   * @return A starting point that is used to calculate elapsed nanoseconds.
   * @see System#nanoTime()
   */
  long getStartNanos();

  /**
   * A size of the samples window.
   *
   * @return {@link Duration} which is not {@linkplain Duration#isZero() zero}
   * and not {@linkplain Duration#isNegative() negative}.
   */
  Duration getSamplesInterval();

  /**
   * Instant that corresponds to the right border of the samples window.
   * At the very beginning this is equal to {@link #getStartNanos()}.
   * This border can be moved to the right by the {@link #tick(long, long)} method.
   *
   * @return The rightmost {@linkplain #tick(long, long) scored} instant.
   */
  long rightSamplesWindowBoundary();

  /**
   * Calculates number of ticks inside the samples window (current ticks).
   *
   * @return Number of current ticks.
   */
  long ticksCount();

  /**
   * Calculates total number of ticks since the {@linkplain #getStartNanos() start}.
   *
   * @return Total number of ticks.
   */
  long ticksTotalCount();

  /**
   * Scores a sample of {@code count} ticks at {@code tNanos} instant.
   * If {@code tNanos} is greater than current {@link #rightSamplesWindowBoundary()}
   * then this method moves the samples window such that its right boundary is at {@code tNanos}.
   *
   * @param count Number of ticks. MAY be negative, zero, or positive.
   * If zero then the method does nothing,
   * otherwise adds {@code count} to the currently scored number of ticks at the specified instant,
   * or just remembers {@code count} ticks if no ticks were scored at the specified instant.
   * @param tNanos Instant at which {@code count} ticks need to be scored.
   * MUST be greater than or equal to {@link #getStartNanos()} because ticks at {@link #getStartNanos()} doesn't count.
   */
  void tick(final long count, final long tNanos);

  /**
   * Calculates average rate of ticks (measured in samplesInterval<sup>-1</sup>)
   * from the {@linkplain #getStartNanos() start}
   * till the {@link #rightSamplesWindowBoundary()}.
   *
   * @return The same value as {@link #rateAverage(long) rateAverage}{@code (}{@link #rightSamplesWindowBoundary()}{@code )}.
   */
  default double rateAverage() {
    return rateAverage(rightSamplesWindowBoundary());
  }

  /**
   * Acts just like {@link #rateAverage()} but the result is measured in {@code unit}<sup>-1</sup>
   * instead of samplesInterval<sup>-1</sup>.
   *
   * @param unit A time interval to use as a unit.
   * MUST NOT be {@linkplain Duration#isZero() zero} or {@linkplain Duration#isNegative() negative}.
   * @return Average rate of ticks measured in {@code unit}<sup>-1</sup>.
   */
  default double rateAverage(final Duration unit) {
    checkNotNull(unit, "unit");
    checkArgument(!unit.isZero(), "unit", "Must not be zero");
    checkArgument(!unit.isNegative(), "unit", "Must be positive");
    return rateAverage() / ((double) getSamplesInterval().toNanos() / unit.toNanos());
  }

  /**
   * Calculates average rate of ticks (measured in samplesInterval<sup>-1</sup>)
   * from the {@linkplain #getStartNanos() start} till the {@code tNanos}.
   *
   * @param tNanos MUST NOT be less than {@link #rightSamplesWindowBoundary()}.
   * @return Average rate of ticks.
   */
  double rateAverage(long tNanos);

  /**
   * Acts just like {@link #rateAverage(long)} but the result is measured in {@code unit}<sup>-1</sup>
   * instead of samplesInterval<sup>-1</sup>.
   *
   * @param tNanos MUST NOT be less than {@link #rightSamplesWindowBoundary()}.
   * @param unit A time interval to use as a unit.
   * MUST NOT be {@linkplain Duration#isZero() zero} or {@linkplain Duration#isNegative() negative}.
   * @return Average rate of ticks measured in {@code unit}<sup>-1</sup>.
   */
  default double rateAverage(final long tNanos, final Duration unit) {
    checkNotNull(unit, "unit");
    checkArgument(!unit.isZero(), "unit", "Must not be zero");
    checkArgument(!unit.isNegative(), "unit", "Must be positive");
    return rateAverage(tNanos) / ((double) getSamplesInterval().toNanos() / unit.toNanos());
  }

  /**
   * Calculates current rate of ticks.
   * Current rate is the ratio of {@link #ticksCount()} to {@link #getSamplesInterval()}
   * measured in samplesInterval<sup>-1</sup>,
   * so essentially this method returns the same value as {@link #ticksCount()}.
   *
   * @return The same value as {@link #ticksCount()} and the same value as
   * {@link #rate(long) rate}{@code (}{@link #rightSamplesWindowBoundary()}{@code )}.
   */
  default double rate() {
    return ticksCount();
  }

  /**
   * Acts just like {@link #rate()} but the result is measured in {@code unit}<sup>-1</sup>
   * instead of samplesInterval<sup>-1</sup>.
   *
   * @param unit A time interval to use as a unit.
   * MUST NOT be {@linkplain Duration#isZero() zero} or {@linkplain Duration#isNegative() negative}.
   * @return Current rate of ticks measured in {@code unit}<sup>-1</sup>.
   */
  default double rate(final Duration unit) {
    checkNotNull(unit, "unit");
    checkArgument(!unit.isZero(), "unit", "Must not be zero");
    checkArgument(!unit.isNegative(), "unit", "Must be positive");
    return rate() / ((double) getSamplesInterval().toNanos() / unit.toNanos());
  }

  /**
   * Calculates rate of ticks (measured in samplesInterval<sup>-1</sup>)
   * as if {@code tNanos} were the right boundary of a samples window
   * (for cases when {@code tNanos} is greater than {@link #rightSamplesWindowBoundary()}),
   * or returns {@link #rateAverage()}.
   *
   * @param tNanos The right boundary of a samples window.
   * @return If {@code tNanos} is greater than {@link #rightSamplesWindowBoundary()} then returns
   * current rate of ticks as if {@code tNanos} were the right boundary of a samples window,
   * otherwise returns {@link #rateAverage()}.
   */
  double rate(long tNanos);

  /**
   * Acts just like {@link #rate(long)} but the result is measured in {@code unit}<sup>-1</sup>
   * instead of samplesInterval<sup>-1</sup>.
   *
   * @param tNanos MUST NOT be less than {@link #rightSamplesWindowBoundary()}.
   * @param unit A time interval to use as a unit.
   * MUST NOT be {@linkplain Duration#isZero() zero} or {@linkplain Duration#isNegative() negative}.
   * @return Current rate of ticks measured in {@code unit}<sup>-1</sup>.
   */
  default double rate(final long tNanos, final Duration unit) {
    checkNotNull(unit, "unit");
    checkArgument(!unit.isZero(), "unit", "Must not be zero");
    checkArgument(!unit.isNegative(), "unit", "Must be positive");
    return rate(tNanos) / ((double) getSamplesInterval().toNanos() / unit.toNanos());
  }
}