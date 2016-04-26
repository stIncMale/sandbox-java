package stinc.male.sandbox.ratexecutor;

import java.time.Duration;

/**
 * A utility that measures rate of {@linkplain #tick(long, long) ticks}.
 * <p>
 * <b>Glossary</b><br>
 * <i>Instant</i><br>
 * {@link RateSampler} treats instants
 * as the number of nanoseconds elapsed since the {@linkplain #getStartNanos() start}.
 * So instant is a pair (startNanos, tNanos), but because startNanos is known and fixed,
 * tNanos is enough to specify an instant. All nanosecond values are compared as specified by {@link System#nanoTime()}.
 * <p>
 * If startNanos is negative or zero then allowed tNanos are in closed interval<br>
 * [startNanos; startNanos + {@link Long#MAX_VALUE}],<br>
 * if startNanos is positive then allowed tNanos are in the union of closed intervals<br>
 * 	[startNanos; {@link Long#MAX_VALUE}]\u222a[{@link Long#MIN_VALUE}; {@link Long#MIN_VALUE} + startNanos - 1]<br>
 * (note that {@link Long#MIN_VALUE} is greater than {@link Long#MAX_VALUE} according to {@link System#nanoTime()}).
 * <p>
 * <i>Sample window</i><br>
 * Sample window is a half-closed time interval
 * ({@linkplain #rightSampleWindowBoundary() rightmostScoredInstant} - {@linkplain #getSampleInterval() sampleInterval}; rightmostScoredInstant].
 * Current ticks are those inside the sample window.
 * Current rate is calculated from current ticks only and by default is measured in sampleInterval<sup>-1</sup>.
 * <p>
 * For example if sampleInterval is 3s,<br>
 * startNanos is 1_000_000_000 ns,<br>
 * and the only scored ticks are<br>
 * 1 at 2_500_000_000 ns (this is just tNanos, not tNanos - startNanos),<br>
 * 1 at 3_000_000_000 ns,<br>
 * 8 at 5_000_000_000 ns,<br>
 * -2 at 6_000_000_000 ns,<br>
 * then the current rate is<br>
 * (8 - 2) / sampleInterval = 6 sampleInterval<sup>-1</sup> = 2 s<sup>-1</sup> because sampleInterval is 3s.
 * <pre>
 *             2_500_000_000 ns                   5_000_000_000 ns
 *                    |                                  |                                       t
 * ----|---------|----1----1---------|---------8--------(-2)------|---------|---------|---------&gt;
 *     |                   |                             |
 * startNanos       3_000_000_000 ns              6_000_000_000 ns
 *                         (--------sample window--------]
 * </pre>
 */
public interface RateSampler {
	/**
	 * See {@link System#nanoTime()}.
	 * @return
	 * A starting point that is used for elapsed nanoseconds.
	 */
	long getStartNanos();

	/**
	 * A size of the sample window.
	 * @return
	 * {@link Duration} which is not {@linkplain Duration#isZero() zero} and not {@linkplain Duration#isNegative() negative}.
	 */
	Duration getSampleInterval();

	/**
	 * Instant that corresponds to the right border of the sample window.
	 * At the very beginning this is equal to {@link #getStartNanos()}.
	 * @return
	 * The rightmost {@linkplain #tick(long, long) scored} instant.
	 */
	long rightSampleWindowBoundary();

	/**
	 * Calculates number of ticks inside the sample window (current ticks).
	 * @return
	 * Number of current ticks.
	 */
	long numberOfCurrentTicks();

	/**
	 * Scores a sample of {@code count} ticks at {@code tNanos} instant.
	 * If {@code tNanos} is bigger than current {@link #rightSampleWindowBoundary()}
	 * then this method moves the sample window such that its right boundary is at {@code tNanos}.
	 * @param count
	 * Number of ticks. MAY be negative, zero, or positive.
	 * If zero then the method does nothing,
	 * otherwise adds {@code count} to the currently scored number of ticks at the specified instant,
	 * or just remembers {@code count} ticks is no ticks were scored at the specified instant.
	 * @param tNanos
	 * Instant at which {@code count} ticks need to be scored.
	 */
	void tick(final long count, final long tNanos);

	/**
	 * Calculates average rate of ticks (measured in sampleInterval<sup>-1</sup>)
	 * from the {@linkplain #getStartNanos() start}
	 * till the {@link #rightSampleWindowBoundary()}.
	 * @return
	 * The same value as {@link #rateAverage(long) rateAverage}{@code (}{@link #rightSampleWindowBoundary()}{@code )}.
	 */
	double rateAverage();

	/**
	 * Calculates average rate of ticks (measured in sampleInterval<sup>-1</sup>)
	 * from the {@linkplain #getStartNanos() start} till the {@code tNanos}.
	 * @param tNanos
	 * MUST NOT be less than {@link #rightSampleWindowBoundary()}.
	 * @return
	 * Average rate of ticks.
	 */
	double rateAverage(long tNanos);

	//TODO
	double rate();

	double rate(long tNanos);
}