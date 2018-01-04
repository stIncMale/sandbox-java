package stinc.male.sandbox.ratmex.meter;

import stinc.male.sandbox.ratmex.meter.ConcurrentRateMeterConfig.Builder;

public final class ConcurrentNavigableMapRateMeterTest extends AbstractRateMeterUnitTest<Builder, ConcurrentRateMeterConfig> {
  public ConcurrentNavigableMapRateMeterTest() {
    super(
        () -> (Builder)ConcurrentNavigableMapRateMeter.defaultConfig()
            .toBuilder()
            .setHistoryLength(2),
        ConcurrentNavigableMapRateMeter::new);
  }
}