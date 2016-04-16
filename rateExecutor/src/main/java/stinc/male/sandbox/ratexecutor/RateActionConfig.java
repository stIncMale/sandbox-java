package stinc.male.sandbox.ratexecutor;

import javax.annotation.concurrent.ThreadSafe;
import static stinc.male.sandbox.ratexecutor.Preconditions.checkNotNull;

/**
 * TODO
 */
@ThreadSafe
public final class RateActionConfig {
	private final DistributionStrategy distributionStrategy;

	RateActionConfig(final DistributionStrategy distributionStrategy) {
		checkNotNull(distributionStrategy, "distributionStrategy");
		this.distributionStrategy = distributionStrategy;
	}

	public final DistributionStrategy getDistributionStrategy() {
		return distributionStrategy;
	}
}