package stinc.male.sandbox.ratmex.meter;

import java.time.Duration;
import static stinc.male.sandbox.ratmex.util.internal.Preconditions.checkNotNull;
import static stinc.male.sandbox.ratmex.util.internal.ConversionsAndChecks.checkTNanos;
import static stinc.male.sandbox.ratmex.util.internal.ConversionsAndChecks.checkUnit;
import static stinc.male.sandbox.ratmex.util.internal.ConversionsAndChecks.convertRate;
import static stinc.male.sandbox.ratmex.util.internal.ConversionsAndChecks.maxTNanos;

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
 * Sample is a pair (tNanos, ticksCount), where ticksCount \u2208 [{@link Long#MIN_VALUE}; {@link Long#MAX_VALUE}].
 * <p>
 * <i>Samples window</i><br>
 * Samples window is a half-closed time interval
 * ({@linkplain #rightSamplesWindowBoundary() rightmostScoredInstant} - {@linkplain #getSamplesInterval() samplesInterval}; rightmostScoredInstant]
 * (comparison according to {@link System#nanoTime()}).
 * Samples window can only be moved to the right and the only way to do this is to call {@link #tick(long, long)}.
 * Current ticks are those inside the samples window.
 * Current rate is calculated base on current ticks only and by default is measured in samplesInterval<sup>-1</sup>.
 * <p>
 * For example if samplesInterval is 30ns,<br>
 * startNanos is 10ns,<br>
 * and the only scored ticks are<br>
 * (25ns, 1) (this is tNanos, not tNanos - startNanos),<br>
 * (30ns, 1),<br>
 * (50ns, 8),<br>
 * (60ns, -2),<br>
 * then the current rate is<br>
 * (8 - 2) / samplesInterval = 6samplesInterval<sup>-1</sup> = 6 / 30ns = 0.2ns<sup>-1</sup>.
 * <pre>
 *              25ns      50ns
 *                    |                        |                                                 t
 * ----|---------|----1----1---------|---------8--------(-2)------|---------|---------|---------&gt;
 *     |                   |                             |
 * startNanos        30ns                     60ns
 *                         (--------samples window-------]
 * </pre>
 * <p>
 * <b>Allowed values</b><br>
 * samplesInterval (in nanos) \u2208 [1, {@link Long#MAX_VALUE} / (HL + 1) - 1],<br>
 * tNanos \u2208 [startNanos, startNanos - (HL + 1) * samplesInterval + {@link Long#MAX_VALUE}]
 * (comparison according to {@link System#nanoTime()}),
 * where HL is the size of samples history maintained by the {@link RateMeter} measured in samplesInterval units.
 * Note that the specification of {@link RateMeter#rate(long)} implies that any {@link RateMeter}
 * must maintain samples history for at least 2samplesInterval.
 * <p>
 * <b>Implementation considerations</b><br>
 * The obvious difficulty in concurrent implementation of this interface is the fact that samples window
 * may be moved by a thread running {@link #tick(long, long)} method, while some other thread
 * tries to count ticks (e.g. {@link #ticksCount()}) or to account new samples.
 * And because it is impossible to always store all the accounted samples,
 * some samples history may be lost while it is still needed, causing some results to be inaccurate.
 * <p>
 * There may be a bunch of challenges like the one mentioned above. Different implementations may
 * conform different correctness conditions (a.k.a. consistency models)
 * and provide different guarantees on the accuracy.
 * Implementations with weaker guarantees may display better performance because they
 * can sacrifice accuracy for the sake of performance and yet may produce sufficiently accurate results in practice.
 * Implementations are recommended to aim for accuracy on the best effort basis, but all {@code ...Count} and all {@code rate...} methods are
 * allowed to produce approximate results. An implementation can report detected inaccuracies via {@link #stats()}.
 * <p>
 * Implementations may not internally use nanosecond {@linkplain #getTimeSensitivity time sensitivity} (resolution, accuracy, granularity). In
 * fact, there is no sense in using resolution better than the resolution of the timer that is used by a user of {@link RateMeter}.
 */
public interface RateMeter {
  /**
   * @return A starting point that is used to calculate elapsed nanoseconds.
   *
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
   * A time sensitivity which affects the behaviour of {@link RateMeter#tick(long, long)} method in a way that allows scoring the specified sample
   * at an instant that differs from the specified one not more than by the time sensitivity, which may be observed via
   * {@link #rightSamplesWindowBoundary()}.
   * <p>
   * This method, just as {@link RateMeterReading#isAccurate()}, can be considered as an important implementation detail leaked through
   * {@link RateMeter}'s <a href="https://www.joelonsoftware.com/2002/11/11/the-law-of-leaky-abstractions/">leaky abstraction</a>.
   * <p>
   * <b>Implementation considerations</b><br>
   * It is recommended to use a resolution (accuracy, granularity) of the timer used by a user of {@link RateMeter}.
   * For example 200ns may be a good approximation of the {@link System#nanoTime()} accuracy
   * (see <a href="https://github.com/shipilev/timers-bench">timers-bench</a>
   * and <a href="https://shipilev.net/blog/2014/nanotrusting-nanotime/">Nanotrusting the Nanotime</a> for measurements and explanations).
   *
   * @return A positive, i.e. not {@linkplain Duration#isNegative() negative} and not {@linkplain Duration#isZero() zero} time sensitivity which
   * is used internally.
   */
  Duration getTimeSensitivity();

  /**
   * Instant that corresponds to the right border of the samples window.
   * At the very beginning this is equal to {@link #getStartNanos()}.
   * This border can be moved to the right by the {@link #tick(long, long)} method.
   *
   * @return The rightmost {@linkplain #tick(long, long) scored} instant.
   */
  long rightSamplesWindowBoundary();

  /**
   * Calculates the number of ticks inside the samples window (current ticks).
   *
   * @return Number of current ticks.
   */
  default long ticksCount() {//TODO remove?
    return ticksCount(new RateMeterReading()).getLongValue();
  }

  RateMeterReading ticksCount(RateMeterReading reading);

  /**
   * Calculates the total number of ticks since the {@linkplain #getStartNanos() start}.
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
   *
   * @return Average rate of ticks measured in {@code unit}<sup>-1</sup>.
   */
  default double rateAverage(final Duration unit) {
    checkUnit(unit, "unit");
    return convertRate(rateAverage(), getSamplesInterval().toNanos(), unit.toNanos());
  }

  /**
   * Calculates average rate of ticks (measured in samplesInterval<sup>-1</sup>)
   * from the {@linkplain #getStartNanos() start} till the {@code tNanos},
   * if {@code tNanos} is greater than {@link #rightSamplesWindowBoundary()},
   * otherwise returns {@link #rateAverage()}.
   *
   * @return Average rate of ticks or 0 if {@code tNanos} is equal to {@link #getStartNanos()}.
   */
  double rateAverage(final long tNanos);

  /**
   * Acts just like {@link #rateAverage(long)} but the result is measured in {@code unit}<sup>-1</sup>
   * instead of samplesInterval<sup>-1</sup>.
   *
   * @param tNanos
   * @param unit A time interval to use as a unit.
   * MUST NOT be {@linkplain Duration#isZero() zero} or {@linkplain Duration#isNegative() negative}.
   *
   * @return Average rate of ticks measured in {@code unit}<sup>-1</sup>.
   */
  default double rateAverage(final long tNanos, final Duration unit) {
    final long startNanos = getStartNanos();
    final long samplesIntervalNanos = getSamplesInterval().toNanos();
    checkTNanos(tNanos, startNanos, maxTNanos(startNanos, samplesIntervalNanos, 3), "tNanos");
    checkUnit(unit, "unit");
    return convertRate(rateAverage(tNanos), getSamplesInterval().toNanos(), unit.toNanos());
  }

  /**
   * Calculates current rate of ticks.
   * Current rate is the ratio of {@link #ticksCount()} to {@link #getSamplesInterval()}
   * measured in samplesInterval<sup>-1</sup>,
   * so this method returns the same value as {@link #ticksCount()},
   * and was added just for the sake of API completeness.
   *
   * @return The same value as {@link #ticksCount()} and
   * {@link #rate(long) rate}{@code (}{@link #rightSamplesWindowBoundary()}{@code )}.
   */
  default long rate() {
    return ticksCount();
  }

  default RateMeterReading rate(final RateMeterReading reading) {
    checkNotNull(reading, "reading");
    return ticksCount(reading);
  }

  /**
   * Acts just like {@link #rate()} but the result is measured in {@code unit}<sup>-1</sup>
   * instead of samplesInterval<sup>-1</sup>.
   *
   * @param unit A time interval to use as a unit.
   * MUST NOT be {@linkplain Duration#isZero() zero} or {@linkplain Duration#isNegative() negative}.
   *
   * @return Current rate of ticks measured in {@code unit}<sup>-1</sup>.
   */
  default double rate(final Duration unit) {
    checkUnit(unit, "unit");
    return convertRate(rate(), getSamplesInterval().toNanos(), unit.toNanos());
  }

  default RateMeterReading rate(final Duration unit, final RateMeterReading reading) {
    checkUnit(unit, "unit");
    checkNotNull(reading, "reading");
    return convertRate(rate(reading), getSamplesInterval().toNanos(), unit.toNanos());
  }

  /**
   * Calculates rate of ticks (measured in samplesInterval<sup>-1</sup>)
   * as if {@code tNanos} were the right boundary of a samples window,
   * if {@code tNanos} is greater than {@link #rightSamplesWindowBoundary()} - {@link #getSamplesInterval()},
   * otherwise returns {@link #rateAverage()}.
   *
   * @param tNanos An effective (imaginary) right boundary of a samples window.
   */
  default double rate(long tNanos) {
    return rate(tNanos, new RateMeterReading()).getDoubleValue();
  }

  RateMeterReading rate(long tNanos, RateMeterReading reading);

  /**
   * Acts just like {@link #rate(long)} but the result is measured in {@code unit}<sup>-1</sup>
   * instead of samplesInterval<sup>-1</sup>.
   *
   * @param tNanos An effective (imaginary) right boundary of a samples window.
   * @param unit A time interval to use as a unit.
   * MUST NOT be {@linkplain Duration#isZero() zero} or {@linkplain Duration#isNegative() negative}.
   *
   * @return Current rate of ticks measured in {@code unit}<sup>-1</sup>.
   */
  default double rate(final long tNanos, final Duration unit) {
    final long startNanos = getStartNanos();
    final long samplesIntervalNanos = getSamplesInterval().toNanos();
    checkTNanos(tNanos, startNanos, maxTNanos(startNanos, samplesIntervalNanos, 3), "tNanos");
    checkUnit(unit, "unit");
    return convertRate(rate(tNanos), samplesIntervalNanos, unit.toNanos());
  }

  default RateMeterReading rate(final long tNanos, final Duration unit, final RateMeterReading reading) {
    final long startNanos = getStartNanos();
    final long samplesIntervalNanos = getSamplesInterval().toNanos();
    checkTNanos(tNanos, startNanos, maxTNanos(startNanos, samplesIntervalNanos, 3), "tNanos");
    checkUnit(unit, "unit");
    checkNotNull(reading, "reading");
    return convertRate(rate(tNanos, reading), samplesIntervalNanos, unit.toNanos());
  }

  /**
   * @return Statistics which may be {@linkplain RateMeterStats#isEmpty() empty} if the {@link RateMeter}
   * does not collects it.
   */
  RateMeterStats stats();
}
//TODO verify javadocs and other comments and messages
//TODO check inheritance/final methods