package stinc.male.sandbox.ratexecutor;

import static stinc.male.sandbox.ratexecutor.Preconditions.checkArgument;
import javax.annotation.concurrent.Immutable;

@Immutable
public final class Shot {
	private final long instantNanos;
	private final long count;

	public Shot(final long instantNanos, final long count) {
		checkArgument("second", count > 0, "count", "The argument must be positive");
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
}
