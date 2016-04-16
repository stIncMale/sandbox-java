package stinc.male.sandbox.ratexecutor;

import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BurstDistributionStrategy implements DistributionStrategy {
	private static final Logger logger = LoggerFactory.getLogger(BurstDistributionStrategy.class);

	BurstDistributionStrategy() {
	}

	@Override
	public final Shot getNext(final ActionCounter rateActionInfo) {
		final Rate rate = rateActionInfo.getRate();
		final long actionsCountPerPeriod = rate.getValue();
		final long activeActionsCount = rateActionInfo.getActiveCount();
		final long completedActionsSincePeriodStartCount = rateActionInfo.getCompletedSincePeriodStartCount();
		final long wantingActionsCountPerPeriod
				= actionsCountPerPeriod - activeActionsCount - completedActionsSincePeriodStartCount;
		logger.trace("TODO actionsCountPerPeriod={}, activeActionsCount={}, completedActionsSincePeriodStartCount={}",
				actionsCountPerPeriod, activeActionsCount, completedActionsSincePeriodStartCount);
		final Shot result;
		if (wantingActionsCountPerPeriod == 0) {
			try {
				TimeUnit.NANOSECONDS.sleep(rateActionInfo.getNanosTillNextPeriod() / 2);//TODO sleep blocks thread
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			result = getNext(rateActionInfo);//TODO remove recurtions
		} else {
			result = new Shot(System.nanoTime(), wantingActionsCountPerPeriod);
		}
		return result;
	}
}