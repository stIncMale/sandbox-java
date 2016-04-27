package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import static stinc.male.sandbox.ratexecutor.Preconditions.checkArgument;
import static stinc.male.sandbox.ratexecutor.Preconditions.checkNotNull;

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
 * otherwise allowed tNanos are in the union of closed intervals<br>
 * [startNanos; {@link Long#MAX_VALUE}]\u222a[{@link Long#MIN_VALUE}; {@link Long#MIN_VALUE} + startNanos - 1]<br>
 * (note that {@link Long#MIN_VALUE} is greater than {@link Long#MAX_VALUE} according to {@link System#nanoTime()}).
 * <p>
 * <i>Sample window</i><br>
 * Sample window is a half-closed time interval defined as:<br>
 * if {@linkplain #rightSampleWindowBoundary() rightmostScoredInstant} - {@linkplain #getSampleInterval() sampleInterval}
 * is less than startNanos then<br>
 * (startNanos; sampleInterval]<br>
 * otherwise<br>
 * (rightmostScoredInstant - sampleInterval; rightmostScoredInstant].
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
 * <b>Performance notes</b><br>
 * This utility is primarily designed to measure current rate by using sample window.
 * This approach introduces performance overhead and although {@link RateSampler} can also measure
 * {@linkplain #rateAverage() average rate} and calculate the {@linkplain #ticksTotalCount() total number of ticks},
 * you MAY want to use more lightweight techniques if you don't need to measure the current rate.
 */
public interface RateSampler {
	/**
	 * See {@link System#nanoTime()}.
	 *
	 * @return A starting point that is used to calculate elapsed nanoseconds.
	 */
	long getStartNanos();

	/**
	 * A size of the sample window.
	 *
	 * @return {@link Duration} which is not {@linkplain Duration#isZero() zero} and not {@linkplain Duration#isNegative() negative}.
	 */
	Duration getSampleInterval();

	/**
	 * Instant that corresponds to the right border of the sample window.
	 * At the very beginning this is equal to {@link #getStartNanos()}.
	 *
	 * @return The rightmost {@linkplain #tick(long, long) scored} instant.
	 */
	long rightSampleWindowBoundary();

	/**
	 * Calculates number of ticks inside the sample window (current ticks).
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
	 * If {@code tNanos} is greater than current {@link #rightSampleWindowBoundary()}
	 * then this method moves the sample window such that its right boundary is at {@code tNanos}.
	 *
	 * @param count  Number of ticks. MAY be negative, zero, or positive.
	 *               If zero or {@link #getStartNanos()} then the method does nothing,
	 *               otherwise adds {@code count} to the currently scored number of ticks at the specified instant,
	 *               or just remembers {@code count} ticks is no ticks were scored at the specified instant.
	 * @param tNanos Instant at which {@code count} ticks need to be scored.
	 *               MUST be greater than or equal to {@link #getStartNanos()} because ticks at {@link #getStartNanos()} doesn't count.
	 */
	void tick(final long count, final long tNanos);

	/**
	 * Calculates average rate of ticks (measured in sampleInterval<sup>-1</sup>)
	 * from the {@linkplain #getStartNanos() start}
	 * till the {@link #rightSampleWindowBoundary()}.
	 *
	 * @return The same value as {@link #rateAverage(long) rateAverage}{@code (}{@link #rightSampleWindowBoundary()}{@code )}.
	 */
	default double rateAverage() {
		return rateAverage(rightSampleWindowBoundary());
	}

	/**
	 * Acts just like {@link #rateAverage()} but the result is measured in {@code unit}<sup>-1</sup>
	 * instead of sampleInterval<sup>-1</sup>.
	 *
	 * @param unit A time interval to use as a unit.
	 *             MUST NOT be {@linkplain Duration#isZero() zero} or {@linkplain Duration#isNegative() negative}.
	 * @return Average rate of ticks measured in {@code unit}<sup>-1</sup>.
	 */
	default double rateAverage(final Duration unit) {
		checkNotNull(unit, "unit");
		checkArgument(!unit.isZero(), "sampleInterval", "Must not be zero");
		checkArgument(!unit.isNegative(), "sampleInterval", "Must be positive");
		return rateAverage() / ((double) getSampleInterval().toNanos() / unit.toNanos());
	}

	/**
	 * Calculates average rate of ticks (measured in sampleInterval<sup>-1</sup>)
	 * from the {@linkplain #getStartNanos() start} till the {@code tNanos}.
	 *
	 * @param tNanos MUST NOT be less than {@link #rightSampleWindowBoundary()}.
	 * @return Average rate of ticks.
	 */
	double rateAverage(long tNanos);

	/**
	 * Acts just like {@link #rateAverage(long)} but the result is measured in {@code unit}<sup>-1</sup>
	 * instead of sampleInterval<sup>-1</sup>.
	 *
	 * @param tNanos MUST NOT be less than {@link #rightSampleWindowBoundary()}.
	 * @param unit   A time interval to use as a unit.
	 *               MUST NOT be {@linkplain Duration#isZero() zero} or {@linkplain Duration#isNegative() negative}.
	 * @return Average rate of ticks measured in {@code unit}<sup>-1</sup>.
	 */
	default double rateAverage(final long tNanos, final Duration unit) {
		checkNotNull(unit, "unit");
		checkArgument(!unit.isZero(), "sampleInterval", "Must not be zero");
		checkArgument(!unit.isNegative(), "sampleInterval", "Must be positive");
		return rateAverage(tNanos) / ((double) getSampleInterval().toNanos() / unit.toNanos());
	}

	/**
	 * Calculates current rate of ticks.
	 * Current rate is the ratio of {@link #ticksCount()} to {@link #getSampleInterval()}
	 * measured in sampleInterval<sup>-1</sup>,
	 * so essentially this method returns the same value as {@link #ticksCount()}.
	 *
	 * @return The same value as {@link #ticksCount()} and the same value as
	 * {@link #rate(long) rate}{@code (}{@link #rightSampleWindowBoundary()}{@code )}.
	 */
	default double rate() {
		return ticksCount();
	}

	/**
	 * Acts just like {@link #rate()} but the result is measured in {@code unit}<sup>-1</sup>
	 * instead of sampleInterval<sup>-1</sup>.
	 *
	 * @param unit A time interval to use as a unit.
	 *             MUST NOT be {@linkplain Duration#isZero() zero} or {@linkplain Duration#isNegative() negative}.
	 * @return Current rate of ticks measured in {@code unit}<sup>-1</sup>.
	 */
	default double rate(final Duration unit) {
		checkNotNull(unit, "unit");
		checkArgument(!unit.isZero(), "sampleInterval", "Must not be zero");
		checkArgument(!unit.isNegative(), "sampleInterval", "Must be positive");
		return rate() / ((double) getSampleInterval().toNanos() / unit.toNanos());
	}

	/**
	 * Calculates rate of ticks (measured in sampleInterval<sup>-1</sup>)
	 * as if {@code tNanos} were the right sample window boundary
	 * (for cases when {@code tNanos} is greater than {@link #rightSampleWindowBoundary()}),
	 * or returns {@link #rateAverage()}.
	 *
	 * @param tNanos The right sample window boundary.
	 * @return If {@code tNanos} is greater than {@link #rightSampleWindowBoundary()} then returns
	 * current rate of ticks as if {@code tNanos} were the right sample window boundary,
	 * otherwise returns {@link #rateAverage()}.
	 */
	double rate(long tNanos);

	/**
	 * Acts just like {@link #rate(long)} but the result is measured in {@code unit}<sup>-1</sup>
	 * instead of sampleInterval<sup>-1</sup>.
	 *
	 * @param tNanos MUST NOT be less than {@link #rightSampleWindowBoundary()}.
	 * @param unit   A time interval to use as a unit.
	 *               MUST NOT be {@linkplain Duration#isZero() zero} or {@linkplain Duration#isNegative() negative}.
	 * @return Current rate of ticks measured in {@code unit}<sup>-1</sup>.
	 */
	default double rate(final long tNanos, final Duration unit) {
		checkNotNull(unit, "unit");
		checkArgument(!unit.isZero(), "sampleInterval", "Must not be zero");
		checkArgument(!unit.isNegative(), "sampleInterval", "Must be positive");
		return rate(tNanos) / ((double) getSampleInterval().toNanos() / unit.toNanos());
	}
}