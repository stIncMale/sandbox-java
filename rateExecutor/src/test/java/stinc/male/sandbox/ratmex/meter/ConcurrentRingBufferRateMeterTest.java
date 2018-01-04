package stinc.male.sandbox.ratmex.meter;

import stinc.male.sandbox.ratmex.meter.ConcurrentRateMeterConfig.Builder;

public final class ConcurrentRingBufferRateMeterTest extends AbstractRateMeterUnitTest<Builder, ConcurrentRateMeterConfig> {
  public ConcurrentRingBufferRateMeterTest() {
    super(
        () -> (Builder)ConcurrentRingBufferRateMeter.defaultConfig()
            .toBuilder()
            .setHistoryLength(2),
        ConcurrentRingBufferRateMeter::new);
  }
}