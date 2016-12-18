package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import static stinc.male.sandbox.ratexecutor.RateMeterMath.checkTNanos;
import static stinc.male.sandbox.ratexecutor.RateMeterMath.checkUnit;
import static stinc.male.sandbox.ratexecutor.RateMeterMath.convertRate;
import static stinc.male.sandbox.ratexecutor.RateMeterMath.maxTNanos;

/**
 * A utility that measures rate of {@linkplain #tick(long, long) ticks}.
 * <p>
 * <b>Glossary</b><br>
 * <i>Instant</i><br>
 * {@link RateMeter} treats instants as the time (number of nanoseconds) elapsed since the {@linkplain #getStartNanos() start}.
 * So instant is a pair (startNanos, elapsedNanos), but because startNanos is known and fixed,
 * we can equivalently specify an instant via a single value tNanos = startNanos + elapsedNanos.
 * {@link RateMeter} uses tNanos notation instead of (startNanos, elapsedNanos) notation.
 * All nanosecond values are compared as specified by {@link System#nanoTime()}.
 * <p>
 * <i>Sample</i><br>
 * Sample is a pair (tNanos, ticksCount), where ticksCount
 * <p>
 * <i>Samples window</i><br>
 * Samples window is a half-closed time interval
 * ({@linkplain #rightSamplesWindowBoundary() rightmostScoredInstant} - {@linkplain #getSamplesInterval() samplesInterval}; rightmostScoredInstant]
 * (comparison according to {@link System#nanoTime()}).
 * Samples window can only be moved to the right and the only way to do this is to call {@link #tick(long, long)}.
 * Current ticks are those inside the samples window.
 * Current rate is calculated base on current ticks only and by default is measured in samplesInterval<sup>-1</sup>.
 * <p>
 * For example if samplesInterval is 3s,<br>
 * startNanos is 1_000_000_000ns,<br>
 * and the only scored ticks are<br>
 * 1 at 2_500_000_000ns (this is tNanos, not tNanos - startNanos),<br>
 * 1 at 3_000_000_000ns,<br>
 * 8 at 5_000_000_000ns,<br>
 * -2 at 6_000_000_000ns,<br>
 * then the current rate is<br>
 * (8 - 2) / samplesInterval = 6samplesInterval<sup>-1</sup> = 2s<sup>-1</sup> because samplesInterval is 3s.
 * <pre>
 *             2_500_000_000ns          5_000_000_000ns
 *                    |                        |                                                 t
 * ----|---------|----1----1---------|---------8--------(-2)------|---------|---------|---------&gt;
 *     |                   |                             |
 * startNanos       3_000_000_000ns               6_000_000_000ns
 *                         (--------samples window-------]
 * </pre>
 * <p>
 * <b>Allowed values</b><br>
 * samplesInterval (in nanos) \u2208 [1, {@link Long#MAX_VALUE} - 1],<br>
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
   * @param tNanos An instant (a pair (startNanos, elapsedNanos)) at which {@code count} ticks need to be scored.
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
    checkUnit(unit, "unit");
    return convertRate(rateAverage(), getSamplesInterval().toNanos(), unit.toNanos());
  }

  /**
   * Calculates average rate of ticks (measured in samplesInterval<sup>-1</sup>)
   * from the {@linkplain #getStartNanos() start} till the {@code tNanos}.
   * Note that this method MAY produce an overvalued result if {@code tNanos} is lower than {@link #rightSamplesWindowBoundary()}.
   *
   * @return Average rate of ticks or 0 if {@code tNanos} is equal to {@link #getStartNanos()}.
   */
  default double rateAverage(final long tNanos) {
    final long startNanos = getStartNanos();
    final long samplesIntervalNanos = getSamplesInterval().toNanos();
    checkTNanos(tNanos, startNanos, maxTNanos(startNanos, samplesIntervalNanos), "tNanos");
    return RateMeterMath.rateAverage(tNanos, samplesIntervalNanos, startNanos, ticksTotalCount());
  }

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
    final long startNanos = getStartNanos();
    final long samplesIntervalNanos = getSamplesInterval().toNanos();
    checkTNanos(tNanos, startNanos, maxTNanos(startNanos, samplesIntervalNanos), "tNanos");
    checkUnit(unit, "unit");
    return convertRate(rateAverage(tNanos), getSamplesInterval().toNanos(), unit.toNanos());
  }

  /**
   * Calculates current rate of ticks.
   * Current rate is the ratio of {@link #ticksCount()} to {@link #getSamplesInterval()}
   * measured in samplesInterval<sup>-1</sup>,
   * so this method returns exactly the same value as {@link #ticksCount()},
   * and was added just for the sake of API completeness.
   *
   * @return The same value as {@link #ticksCount()} and essentially the same value as
   * {@link #rate(long) rate}{@code (}{@link #rightSamplesWindowBoundary()}{@code )}.
   */
  default long rate() {
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
    checkUnit(unit, "unit");
    return convertRate(rate(), getSamplesInterval().toNanos(), unit.toNanos());
  }

  /**
   * Calculates rate of ticks (measured in samplesInterval<sup>-1</sup>)
   * as if {@code tNanos} were the right boundary of a samples window
   * if {@code tNanos} is greater than or equal to {@link #rightSamplesWindowBoundary()},
   * otherwise returns {@link #rateAverage()}.
   *
   * @param tNanos The right boundary of a samples window.
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
    final long startNanos = getStartNanos();
    final long samplesIntervalNanos = getSamplesInterval().toNanos();
    checkTNanos(tNanos, startNanos, maxTNanos(startNanos, samplesIntervalNanos), "tNanos");
    checkUnit(unit, "unit");
    return convertRate(rate(tNanos), samplesIntervalNanos, unit.toNanos());
  }
}