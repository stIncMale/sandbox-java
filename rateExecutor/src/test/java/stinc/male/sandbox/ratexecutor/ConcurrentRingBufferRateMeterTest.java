package stinc.male.sandbox.ratexecutor;

import stinc.male.sandbox.ratexecutor.ConcurrentRingBufferRateMeterConfig.Builder;
import stinc.male.sandbox.ratexecutor.test.harness.AbstractRateMeterUnitTest;

public final class ConcurrentRingBufferRateMeterTest extends AbstractRateMeterUnitTest<Builder, ConcurrentRingBufferRateMeterConfig> {
  public ConcurrentRingBufferRateMeterTest() {
    super(
        ConcurrentRingBufferRateMeterConfig.Builder::new,
        ConcurrentRingBufferRateMeter::new);
  }
}