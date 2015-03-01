package stinc.male.sandbox.ratexecutor;

import static stinc.male.sandbox.ratexecutor.Preconditions.checkNotNull;
import javax.annotation.concurrent.ThreadSafe;

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
