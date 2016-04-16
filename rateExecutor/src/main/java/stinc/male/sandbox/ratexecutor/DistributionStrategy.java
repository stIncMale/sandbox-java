package stinc.male.sandbox.ratexecutor;

import javax.annotation.concurrent.ThreadSafe;

@FunctionalInterface
@ThreadSafe
public interface DistributionStrategy {
	Shot getNext(ActionCounter rateActionInfo);
}