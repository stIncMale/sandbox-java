package stinc.male.sandbox.ratexecutor;

<<<<<<< d39cbd0aa262fc78a8a7734a038ead42ad99e74a
import static stinc.male.sandbox.ratexecutor.Preconditions.checkArgument;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
final class ActionCounter {
	private final long startNanos;
	private final long periodNanos;
	private final LongAdder activeCounter;
	private volatile LongAdder completedSincePeriodStartCounter;
	private final AtomicLong lastPeriodIndex;

	ActionCounter(final long startNanos, final long periodNanos) {
		checkArgument("second", periodNanos > 0, "periodNanos", "The argument must be positive");
		this.startNanos = startNanos;
		this.periodNanos = periodNanos;
		activeCounter = new LongAdder();
		completedSincePeriodStartCounter = new LongAdder();
		lastPeriodIndex = new AtomicLong();
	}

	public final void countStarted() {
		activeCounter.increment();
	}

	public final void countCompleted() {
		final long lastPeriodIndexValue = lastPeriodIndex.get();
		final long periodIndex = (System.nanoTime() - startNanos) / periodNanos;
		if (periodIndex > lastPeriodIndexValue) {
			if (lastPeriodIndex.compareAndSet(lastPeriodIndexValue, periodIndex)) {
				completedSincePeriodStartCounter = new LongAdder();
			}
		}
		completedSincePeriodStartCounter.increment();
		activeCounter.decrement();
	}

	public final long getActive() {
		return activeCounter.sum();
	}

	public final long getCompletedSincePeriodStart() {
		return completedSincePeriodStartCounter.sum();
	}
=======
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public interface ActionCounter {
	Rate getRate();

	long getActiveCount();

	long getCompletedSincePeriodStartCount();

	long getPeriodIndex();

	long getNanosTillNextPeriod();
>>>>>>> Create sandbox-rateExecutor project
}
