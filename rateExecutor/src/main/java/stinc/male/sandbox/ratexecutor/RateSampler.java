package stinc.male.sandbox.ratexecutor;

public interface RateSampler {
	double tick(final long count, final long instantNanos);

	double rate();
}