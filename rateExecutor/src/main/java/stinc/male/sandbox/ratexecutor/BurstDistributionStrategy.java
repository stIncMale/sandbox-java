package stinc.male.sandbox.ratexecutor;

<<<<<<< d39cbd0aa262fc78a8a7734a038ead42ad99e74a
final class BurstDistributionStrategy implements DistributionStrategy {
=======
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BurstDistributionStrategy implements DistributionStrategy {
	private static final Logger logger = LoggerFactory.getLogger(BurstDistributionStrategy.class);

>>>>>>> Create sandbox-rateExecutor project
	BurstDistributionStrategy() {
	}

	@Override
<<<<<<< d39cbd0aa262fc78a8a7734a038ead42ad99e74a
	public final Shot getNext(final RateActionInfo rateActionInfo) {
		final Rate rate = rateActionInfo.getRate();
		final long actionsCountPerPeriod = rate.getValue();
		final long periodNanos = rate.getPeriodNanos();
		final long wantingActionsCountPerPeriod
				= actionsCountPerPeriod - rateActionInfo.getActiveCount() - rateActionInfo.getCompletedSincePeriodStartCount();
		return new Shot(System.nanoTime(), wantingActionsCountPerPeriod);
=======
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
>>>>>>> Create sandbox-rateExecutor project
	}
}
