package stinc.male.sandbox.ratexecutor;

public interface RateSampler {
	void tick(final long count, final long tNanos);

	double rateAverage();

	double rateAverage(long tNanos);

	double rate();

	double rate(long tNanos);
}