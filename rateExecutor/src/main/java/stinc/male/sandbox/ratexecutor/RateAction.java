package stinc.male.sandbox.ratexecutor;

import static stinc.male.sandbox.ratexecutor.Preconditions.checkNotNull;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
final class RateAction implements Runnable, RateActionInfo {
	private final Runnable action;
	private final Rate rate;
	private final ActionCounter counter;

	RateAction(final Runnable action, final Rate rate, final long startNanos) {
		checkNotNull("first", action, "action");
		checkNotNull("second", rate, "rate");
		this.action = action;
		this.rate = rate;
		this.counter = new InternalActionCounter(startNanos, rate);
	}

	@Override
	public final void run() {
		counter.countStarted();
		try {
			action.run();
		} finally {
			counter.countCompleted();
		}
	}

	@Override
	public final Rate getRate() {
		return rate;
	}

	@Override
	public final long getActiveCount() {
		return counter.getActive();
	}

	@Override
	public final long getCompletedSincePeriodStartCount() {
		return counter.getCompletedSincePeriodStart();
	}
}
