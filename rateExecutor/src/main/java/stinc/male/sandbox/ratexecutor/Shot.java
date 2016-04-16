package stinc.male.sandbox.ratexecutor;

import javax.annotation.concurrent.Immutable;
import static stinc.male.sandbox.ratexecutor.Preconditions.checkArgument;

@Immutable
public final class Shot {
	private final long instantNanos;
	private final long count;

	public Shot(final long instantNanos, final long count) {
		checkArgument("second", count >= 0, "count", "The argument must be greater than or equal to 0");
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

	@Override
	public final String toString() {
		final String result = new StringBuilder(getClass().getSimpleName())
				.append("(instantNanos=").append(instantNanos)
				.append(", count=").append(count).append(')')
				.toString();
		return result;
	}
}