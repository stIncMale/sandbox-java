package stinc.male.sandbox.ratmex.meter;

import stinc.male.sandbox.ratmex.meter.RateMeterConfig.Builder;

public final class RingBufferRateMeterTest extends AbstractRateMeterUnitTest<Builder, RateMeterConfig> {
  public RingBufferRateMeterTest() {
    super(
        () -> RingBufferRateMeter.defaultConfig()
            .toBuilder()
            .setHistoryLength(2),
        RingBufferRateMeter::new);
  }
}