package stinc.male.sandbox.ratexecutor;

import stinc.male.sandbox.ratexecutor.RateMeterConfig.Builder;
import stinc.male.sandbox.ratexecutor.test.harness.AbstractRateMeterUnitTest;

public final class RingBufferRateMeterTest extends AbstractRateMeterUnitTest<Builder, RateMeterConfig> {
  public RingBufferRateMeterTest() {
    super(
        RateMeterConfig.Builder::new,
        RingBufferRateMeter::new);
  }
}