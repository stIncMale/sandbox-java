package stinc.male.sandbox.ratmex.meter;

import stinc.male.sandbox.ratmex.meter.ConcurrentRingBufferRateMeterConfig.Builder;

public final class ConcurrentRingBufferRateMeterTest extends AbstractRateMeterUnitTest<Builder, ConcurrentRingBufferRateMeterConfig> {
  public ConcurrentRingBufferRateMeterTest() {
    super(
        ConcurrentRingBufferRateMeterConfig.Builder::new,
        ConcurrentRingBufferRateMeter::new);
  }
}