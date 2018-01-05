package stinc.male.sandbox.ratmex.meter;

import java.time.Duration;
import java.util.Optional;
import static stinc.male.sandbox.ratmex.internal.util.Preconditions.checkNotNull;
import static stinc.male.sandbox.ratmex.internal.util.ConversionsAndChecks.checkTNanos;
import static stinc.male.sandbox.ratmex.internal.util.ConversionsAndChecks.checkUnit;
import static stinc.male.sandbox.ratmex.internal.util.ConversionsAndChecks.convertRate;
import static stinc.male.sandbox.ratmex.internal.util.ConversionsAndChecks.maxTNanos;

/**
 * A utility that measures rate of ticks.
 * <p>
 * <b>Glossary</b><br>
 * <i>Tick</i><br>
 * A tick is any event which is registered with {@link #tick(long, long)} method.
 * <p>
 * <i>Instant</i><br>
 * {@link RateMeter} treats instants as the time (number of nanoseconds) elapsed since the {@linkplain #getStartNanos() start}.
 * So an instant is a pair (startNanos, elapsedNanos), but because startNanos is known and fixed,
 * we can equivalently specify an instant via a single value tNanos = startNanos + elapsedNanos (note that tNanos >= startNanos).
 * {@link RateMeter} uses tNanos notation instead of (startNanos, elapsedNanos) notation.
 * All nanosecond values are compared as specified by {@link System#nanoTime()}.
 * <p>
 * <i>Sample</i><br>
 * A sample is a pair (tNanos, ticksCount), where ticksCount \u2208 [{@link Long#MIN_VALUE}; {@link Long#MAX_VALUE}].
 * <p>
 * <i>Samples window</i><br>
 * A samples window is a half-closed time interval
 * ({@linkplain #rightSamplesWindowBoundary() rightmostScoredInstant} - {@linkplain #getSamplesInterval() samplesInterval}; rightmostScoredInstant]
 * (comparison according to {@link System#nanoTime()}).
 * Samples window can only be moved to the right and the only way to do this is to call {@link #tick(long, long)}.
 * <p>
 * <i>Current ticks</i><br>
 * Current ticks are those registered inside the current samples window.
 * <p>
 * <i>Rate</i><br>
 * A rate (or more precisely an instant, or current rate) is calculated based on current ticks
 * and by default is measured in samplesInterval<sup>-1</sup>, hence current rate value always equals to the current ticks count.
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
 * samplesInterval (in nanos) \u2208 [1, {@link Long#MAX_VALUE} / (historyLength + 1) - 1],<br>
 * tNanos \u2208 [startNanos, startNanos - (historyLength + 1) * samplesInterval + {@link Long#MAX_VALUE}]
 * (comparison according to {@link System#nanoTime()}),
 * where historyLength is the length of samples history maintained by the {@link RateMeter} measured in samplesInterval units.
 * Note that the specification of {@link RateMeter#rate(long)} implies that any {@link RateMeter}
 * must maintain samples history for at least 2samplesInterval.
 * <p>
 * <b>API notes</b><br>
 * This interface is designed to allow garbage-free implementations and usage scenarios.
 * Methods with {@link RateMeterReading} parameter are specifically serve this purpose:
 * they do not create {@link RateMeterReading} objects, thus allowing a user to reuse the same {@link RateMeterReading}.
 * <p>
 * <b>Implementation notes</b><br>
 * The obvious difficulty in concurrent implementation of this interface is the fact that samples window
 * may be moved by a thread running {@link #tick(long, long)} method, while some other thread
 * tries to count ticks (e.g. {@link #ticksCount()}) or to register new samples.
 * And because it is impossible to always store all the registered samples,
 * some samples history may be lost while it is still needed, causing some results to be inaccurate.
 * <p>
 * There may be a bunch of challenges like the one mentioned above. Different implementations may
 * conform different correctness conditions (a.k.a. consistency models)
 * and provide different guarantees on the accuracy.
 * Implementations with weaker guarantees may display better performance because they
 * can sacrifice accuracy for the sake of performance and yet may produce sufficiently accurate results in practice.
 * Implementations are recommended to aim for accuracy on the best effort basis,
 * but all methods which have analogs with {@link RateMeterReading} parameters are allowed to produce approximate results.
 * An implementation should report detected inaccuracies via {@link #stats()} and {@link RateMeterReading}.
 * <p>
 * Implementations may not internally use nanosecond {@linkplain #getTimeSensitivity time sensitivity} (resolution, accuracy, granularity). In
 * fact, there is no sense in using resolution better than the resolution of the timer that is used by a user of {@link RateMeter}.
 *
 * @param <S> A type that represents {@linkplain #stats() statistics}.
 */
public interface RateMeter<S> {
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
   * This method can be considered as an important implementation detail leaked through {@link RateMeter}'s
   * <a href="https://www.joelonsoftware.com/2002/11/11/the-law-of-leaky-abstractions/">leaky abstraction</a>.
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
  default long ticksCount() {
    return ticksCount(new RateMeterReading()).getValueLong();
  }

  /**
   * This method is equivalent to {@link #ticksCount()}, but provides a complete measured data,
   * while {@link #ticksCount()} returns only the current ticks count.
   *
   * @param reading A {@link RateMeterReading} to be filled with a measured data.
   *
   * @return {@code reading} filled with the measured data.
   */
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
   * @param count Number of ticks. May be negative, zero, or positive.
   * If zero then the method does nothing,
   * otherwise adds {@code count} to the currently scored number of ticks at the specified instant,
   * or just remembers {@code count} ticks if no ticks were scored at the specified instant.
   * @param tNanos An instant (a pair (startNanos, elapsedNanos)) at which {@code count} ticks need to be scored.
   */
  void tick(final long count, final long tNanos);

  /**
   * Calculates an average (mean) rate of ticks (measured in samplesInterval<sup>-1</sup>)
   * from the {@linkplain #getStartNanos() start}
   * till the {@link #rightSamplesWindowBoundary()}.
   *
   * @return The same value as {@link #rateAverage(long) rateAverage}{@code (}{@link #rightSamplesWindowBoundary()}{@code )}.
   */
  default double rateAverage() {
    return rateAverage(rightSamplesWindowBoundary());
  }

  /**
   * This method is equivalent to {@link #rateAverage()}
   * but the result is measured in {@code unit}<sup>-1</sup> instead of samplesInterval<sup>-1</sup>.
   *
   * @param unit A time interval to use as a unit.
   * Must not be {@linkplain Duration#isZero() zero} or {@linkplain Duration#isNegative() negative}.
   *
   * @return Average rate of ticks measured in {@code unit}<sup>-1</sup>.
   */
  default double rateAverage(final Duration unit) {
    checkUnit(unit, "unit");
    return convertRate(rateAverage(), getSamplesInterval().toNanos(), unit.toNanos());
  }

  /**
   * Calculates an average (mean) rate of ticks (measured in samplesInterval<sup>-1</sup>)
   * from the {@linkplain #getStartNanos() start} till the {@code tNanos},
   * if {@code tNanos} is greater than {@link #rightSamplesWindowBoundary()},
   * otherwise returns {@link #rateAverage()}.
   *
   * @param tNanos An effective (imaginary) right boundary of the samples window.
   *
   * @return Average rate of ticks or 0 if {@code tNanos} is equal to {@link #getStartNanos()}.
   */
  double rateAverage(final long tNanos);

  /**
   * This method is equivalent to {@link #rateAverage(long)}
   * but the result is measured in {@code unit}<sup>-1</sup> instead of samplesInterval<sup>-1</sup>.
   *
   * @param tNanos An effective (imaginary) right boundary of the samples window.
   * @param unit A time interval to use as a unit.
   * Must not be {@linkplain Duration#isZero() zero} or {@linkplain Duration#isNegative() negative}.
   *
   * @return Average rate of ticks measured in {@code unit}<sup>-1</sup>.
   */
  double rateAverage(final long tNanos, final Duration unit);

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

  /**
   * This method is equivalent to {@link #rate()}, but provides a complete measured data,
   * while {@link #rate()} returns only a rate value.
   *
   * @param reading A {@link RateMeterReading} to be filled with a measured data.
   *
   * @return {@code reading} filled with the measured data.
   */
  default RateMeterReading rate(final RateMeterReading reading) {
    checkNotNull(reading, "reading");
    return ticksCount(reading);
  }

  /**
   * This method is equivalent to {@link #rate()}
   * but the result is measured in {@code unit}<sup>-1</sup> instead of samplesInterval<sup>-1</sup>.
   *
   * @param unit A time interval to use as a unit.
   * Must not be {@linkplain Duration#isZero() zero} or {@linkplain Duration#isNegative() negative}.
   *
   * @return Current rate of ticks measured in {@code unit}<sup>-1</sup>.
   */
  default double rate(final Duration unit) {
    checkUnit(unit, "unit");
    return convertRate(rate(), getSamplesInterval().toNanos(), unit.toNanos());
  }

  /**
   * This method is equivalent to {@link #rate(Duration)}, but provides a complete measured data,
   * while {@link #rate(Duration)} returns only a rate value.
   *
   * @param unit A time interval to use as a unit.
   * Must not be {@linkplain Duration#isZero() zero} or {@linkplain Duration#isNegative() negative}.
   * @param reading A {@link RateMeterReading} to be filled with a measured data.
   *
   * @return {@code reading} filled with the measured data.
   */
  default RateMeterReading rate(final Duration unit, final RateMeterReading reading) {
    checkUnit(unit, "unit");
    checkNotNull(reading, "reading");
    final Duration samplesInterval = getSamplesInterval();
    return unit.equals(samplesInterval)
        ? rate(reading)
        : convertRate(rate(reading), samplesInterval.toNanos(), unit.toNanos());
  }

  /**
   * Calculates rate of ticks (measured in samplesInterval<sup>-1</sup>)
   * as if {@code tNanos} were the right boundary of the samples window,
   * if {@code tNanos} is ahead of or within the samples history
   * (i.e. in the most conservative case ahead of {@link #rightSamplesWindowBoundary()} - {@link #getSamplesInterval()},
   * but an implementation may maintain samples history longer than 2samplesInterval, thus relaxing this boundary),
   * otherwise returns {@link #rateAverage()}.
   *
   * @param tNanos An effective (imaginary) right boundary of the samples window.
   */
  default double rate(long tNanos) {
    return rate(tNanos, new RateMeterReading()).getValueDouble();
  }

  /**
   * This method is equivalent to {@link #rate(long)}, but provides a complete measured data,
   * while {@link #rate(long)} returns only a rate value.
   *
   * @param tNanos An effective (imaginary) right boundary of the samples window.
   * @param reading A {@link RateMeterReading} to be filled with a measured data.
   *
   * @return {@code reading} filled with the measured data.
   */
  RateMeterReading rate(long tNanos, RateMeterReading reading);

  /**
   * This method is equivalent to {@link #rate(long)}
   * but the result is measured in {@code unit}<sup>-1</sup> instead of samplesInterval<sup>-1</sup>.
   *
   * @param tNanos An effective (imaginary) right boundary of the samples window.
   * @param unit A time interval to use as a unit.
   * Must not be {@linkplain Duration#isZero() zero} or {@linkplain Duration#isNegative() negative}.
   *
   * @return Current rate of ticks measured in {@code unit}<sup>-1</sup>.
   */
  double rate(final long tNanos, final Duration unit);

  /**
   * This method is equivalent to {@link #rate(long, Duration)}, but provides a complete measured data,
   * while {@link #rate(long, Duration)} returns only a rate value.
   *
   * @param tNanos An effective (imaginary) right boundary of the samples window.
   * @param unit A time interval to use as a unit.
   * Must not be {@linkplain Duration#isZero() zero} or {@linkplain Duration#isNegative() negative}.
   * @param reading A {@link RateMeterReading} to be filled with a measured data.
   *
   * @return {@code reading} filled with the measured data.
   */
  default RateMeterReading rate(final long tNanos, final Duration unit, final RateMeterReading reading) {
    final long startNanos = getStartNanos();
    final Duration samplesInterval = getSamplesInterval();
    final long samplesIntervalNanos = getSamplesInterval().toNanos();
    checkTNanos(tNanos, startNanos, maxTNanos(startNanos, samplesIntervalNanos, 3), "tNanos");
    checkUnit(unit, "unit");
    checkNotNull(reading, "reading");
    return unit.equals(samplesInterval)
        ? rate(tNanos, reading)
        : convertRate(rate(tNanos, reading), samplesIntervalNanos, unit.toNanos());
  }

  /**
   * @return Stats which may be not {@linkplain Optional#isPresent() present}
   * if the {@link RateMeter} does not collect stats.
   */
  Optional<S> stats();
}