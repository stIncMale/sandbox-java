package stinc.male.sandbox.ratexecutor;

final class BurstDistributionStrategy implements DistributionStrategy {
	BurstDistributionStrategy() {
	}

	@Override
	public final Shot getNext(final RateActionInfo rateActionInfo) {
		final Rate rate = rateActionInfo.getRate();
		final long actionsCountPerPeriod = rate.getValue();
		final long periodNanos = rate.getPeriodNanos();
		final long wantingActionsCountPerPeriod
				= actionsCountPerPeriod - rateActionInfo.getActiveCount() - rateActionInfo.getCompletedSincePeriodStartCount();
		return new Shot(System.nanoTime(), wantingActionsCountPerPeriod);
	}
}
