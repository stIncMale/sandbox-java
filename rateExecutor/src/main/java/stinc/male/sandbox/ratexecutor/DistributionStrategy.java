package stinc.male.sandbox.ratexecutor;

import javax.annotation.concurrent.ThreadSafe;

@FunctionalInterface
@ThreadSafe
public interface DistributionStrategy {
<<<<<<< d39cbd0aa262fc78a8a7734a038ead42ad99e74a
	Shot getNext(RateActionInfo rateActionInfo);
=======
	Shot getNext(ActionCounter rateActionInfo);
>>>>>>> Create sandbox-rateExecutor project
}
