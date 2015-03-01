package stinc.male.sandbox.ratexecutor;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public interface RateActionInfo {
	Rate getRate();

	long getActiveCount();

	long getCompletedSincePeriodStartCount();
}
