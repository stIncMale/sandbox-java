package stinc.male.sandbox.ratmex.executor;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public interface RateStrategy {
  void decide(final long ticksCount, final long samplesIntervalNanos, final long tNanos) throws RateStrategyException;
}