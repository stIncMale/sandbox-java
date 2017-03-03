package stinc.male.sandbox.ratexecutor;

import stinc.male.sandbox.ratexecutor.RateMeterConfig.Builder;
import stinc.male.sandbox.ratexecutor.test.harness.AbstractRateMeterUnitTest;

public final class ConcurrentSimpleRateMeterTest extends AbstractRateMeterUnitTest<Builder, RateMeterConfig> {
  public ConcurrentSimpleRateMeterTest() {
    super(
        RateMeterConfig.Builder::new,
        (startNanos, samplesInterval, config) -> new ConcurrentSimpleRateMeter(
            new RingBufferRateMeter(startNanos, samplesInterval, config), new StampedLockingStrategy()));
  }
}