package stinc.male.sandbox.ratexecutor;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public interface ActionCounter {
	Rate getRate();

	long getActiveCount();

	long getCompletedSincePeriodStartCount();

	long getPeriodIndex();

	long getNanosTillNextPeriod();
}