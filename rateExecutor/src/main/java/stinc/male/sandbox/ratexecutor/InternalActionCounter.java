package stinc.male.sandbox.ratexecutor;

import static stinc.male.sandbox.ratexecutor.Preconditions.checkNotNull;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.concurrent.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ThreadSafe
final class InternalActionCounter implements ActionCounter {
	private static final Logger logger = LoggerFactory.getLogger(InternalActionCounter.class);

	private final long startNanos;
	private final Rate rate;
	private final AtomicLong activeCounter;
	private volatile AtomicLong completedSincePeriodStartCounter;
	private final AtomicLong lastSeenPeriodIndex;

	InternalActionCounter(final long startNanos, final Rate rate) {
		checkNotNull("second", rate, "rate");
		this.startNanos = startNanos;
		this.rate = rate;
		activeCounter = new AtomicLong();
		completedSincePeriodStartCounter = new AtomicLong();
		lastSeenPeriodIndex = new AtomicLong();
	}

	public final void countStarted() {
		activeCounter.incrementAndGet();
	}

	public final void countCompleted() {
		updatePeriod();
		completedSincePeriodStartCounter.incrementAndGet();
		activeCounter.decrementAndGet();
	}

	@Override
	public final long getActiveCount() {
		return activeCounter.get();
	}

	@Override
	public final long getCompletedSincePeriodStartCount() {
		final long result = updatePeriod() ? 0 : completedSincePeriodStartCounter.get();
		return result;
	}

	@Override
	public final long getPeriodIndex() {
		final long result = (System.nanoTime() - startNanos) / rate.getPeriodNanos();
		return result;
	}

	@Override
	public final long getNanosTillNextPeriod() {
		final long result = (System.nanoTime() - startNanos) % rate.getPeriodNanos();
		return result;
	}

	@Override
	public final Rate getRate() {
		return rate;
	}

	private final boolean updatePeriod() {
		final boolean result;
		final long lastSeenPeriodIndexValue = lastSeenPeriodIndex.get();
		final long periodIndex = getPeriodIndex();
		if (periodIndex > lastSeenPeriodIndexValue) {
			if (lastSeenPeriodIndex.compareAndSet(lastSeenPeriodIndexValue, periodIndex)) {
				completedSincePeriodStartCounter = new AtomicLong();
				result = true;
			} else {
				result = false;
			}
		} else {
			result = false;
		}
		logger.trace("TODO #{}, completedSincePeriodStartCounter={}",
				lastSeenPeriodIndex.get(), completedSincePeriodStartCounter.get());
		return result;
	}
}
