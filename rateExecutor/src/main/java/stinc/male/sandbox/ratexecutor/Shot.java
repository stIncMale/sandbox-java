package stinc.male.sandbox.ratexecutor;

import static stinc.male.sandbox.ratexecutor.Preconditions.checkArgument;
import javax.annotation.concurrent.Immutable;

@Immutable
public final class Shot {
	private final long instantNanos;
	private final long count;

	public Shot(final long instantNanos, final long count) {
<<<<<<< d39cbd0aa262fc78a8a7734a038ead42ad99e74a
		checkArgument("second", count > 0, "count", "The argument must be positive");
=======
		checkArgument("second", count >= 0, "count", "The argument must be greater than or equal to 0");
>>>>>>> Create sandbox-rateExecutor project
		this.instantNanos = instantNanos;
		this.count = count;
	}

	/**
	 * TODO MAY be negative
	 */
	public final long getInstantNanos() {
		return instantNanos;
	}

	public final long getCount() {
		return count;
	}
<<<<<<< d39cbd0aa262fc78a8a7734a038ead42ad99e74a
=======

	@Override
	public final String toString() {
		final String result = new StringBuilder(getClass().getSimpleName())
				.append("(instantNanos=").append(instantNanos)
				.append(", count=").append(count).append(')')
				.toString();
		return result;
	}
>>>>>>> Create sandbox-rateExecutor project
}
