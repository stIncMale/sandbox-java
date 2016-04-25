package stinc.male.sandbox.ratexecutor;

import java.time.Duration;

/**
 * A utility that measures rate of {@linkplain #tick(long, long) ticks}.
 * <p>
 * {@link RateSampler} treats instants
 * as the number of nanoseconds elapsed since the {@linkplain #getStartNanos() start}.
 * <p>
 * Current values are calculated with respect to the {@linkplain #getSampleInterval() sampleInterval},
 * and are measured in sampleInterval<sup>-1</sup>. For example if sampleInterval is 3s, start is 1_000_000_000 ns,
 * and the only scored ticks are 4 at the 5_000_000_000 ns and 2 at the 6_000_000_000 ns, then the
 */
public interface RateSampler {
	/**
	 * See {@link System#nanoTime()}.
	 * @return
	 * A starting point that is used for elapsed nanoseconds.
	 */
	long getStartNanos();

	/**
	 * All {@linkplain #tick(long, long) ticks} scored in the half-closed interval
	 * (greatestScoredInstant - {@linkplain #getSampleInterval() sampleInterval}; greatestScoredInstant]
	 * (and only such ticks) are used to calculate current values.
	 * @return
	 * An interval which is used to calculate current values.
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
	 * @return
	 */
	long count();
}