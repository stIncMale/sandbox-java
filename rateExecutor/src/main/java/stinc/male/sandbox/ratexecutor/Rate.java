package stinc.male.sandbox.ratexecutor;

import static stinc.male.sandbox.ratexecutor.Preconditions.checkArgument;
import static stinc.male.sandbox.ratexecutor.Preconditions.checkNotNull;
import java.util.concurrent.TimeUnit;
import javax.annotation.concurrent.Immutable;

/**
 * TODO
 */
@Immutable
public final class Rate {
	private final long value;
	private final long period;
	private final long periodNanos;
	private final TimeUnit timeUnit;

	public Rate(final long value, final long period, final TimeUnit timeUnit) {
		checkArgument("first", value > 0, "value", "The argument must be positive");
		checkArgument("second", period > 0, "period", "The argument must be positive");
		checkNotNull("third", timeUnit, "timeUnit");
		this.value = value;
		this.period = period;
		this.timeUnit = timeUnit;
		this.periodNanos = TimeUnit.NANOSECONDS.convert(period, timeUnit);
	}

	public final long getValue() {
		return value;
	}

	public final long getPeriodNanos() {
		return periodNanos;
	}

	@Override
	public final String toString() {
		final StringBuilder sb = new StringBuilder("Rate(")
				.append(value)
				.append(" per ")
				.append(period).append(" ").append(timeUnit).append(')');
		return sb.toString();
	}
}
