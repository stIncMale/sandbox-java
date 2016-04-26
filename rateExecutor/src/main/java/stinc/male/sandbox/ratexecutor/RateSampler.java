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
 * <i>Sample window</i> and <i>sample interval</i><br>
 * Sample window is a half-closed time interval (greatestScoredNanos - sampleInterval; greatestScoredNanos],
 * where {@linkplain #getSampleInterval() sample interval} is just a known fixed value that represents a size of the sample window,
 * and greatestScoredNanos is the bigger known instant scored via the {@link #tick(long, long)} method.
 * <p>
 * Current ticks are those inside the sample window.
 * Current rate is calculated from current ticks only and is measured in sampleInterval<sup>-1</sup>.
 * <p>
 * For example if sampleInterval is 3s,<br>
 * start is 1_000_000_000 ns,<br>
 * and the only scored ticks are<br>
 * 1 at 2_500_000_000 ns (this is just tNanos, not tNanos - startNanos),<br>
 * 1 at 3_000_000_000 ns,<br>
 * 4 at 5_000_000_000 ns,<br>
 * 2 at 6_000_000_000 ns,<br>
 * then the current rate is<br>
 * (4 + 2) / sampleInterval = 6 sampleInterval<sup>-1</sup> or 2 s<sup>-1</sup> because sampleInterval is 3s.
 * <pre>
 *             2_500_000_000 ns                   5_000_000_000 ns
 *                    |                                  |
 * ----|---------|----1----1---------|---------4---------2---------|---------|---------|---------&gt;
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
	 * Scores a sample of {@code count} ticks at {@code tNanos} instant.
	 * @param count
	 * Number of ticks.
	 * @param tNanos
	 * Instant (see {@link System#nanoTime()}).
	 */
	void tick(final long count, final long tNanos);

	double rateAverage();

	double rateAverage(long tNanos);

	double rate();

	double rate(long tNanos);

	/**
	 * Calculates ticks
	 * @return TODO
	 */
	long count();
}