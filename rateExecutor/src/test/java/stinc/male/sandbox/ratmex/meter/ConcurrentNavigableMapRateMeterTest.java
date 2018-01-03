package stinc.male.sandbox.ratmex.meter;

import stinc.male.sandbox.ratmex.meter.RateMeterConfig.Builder;

public final class ConcurrentNavigableMapRateMeterTest extends AbstractRateMeterUnitTest<Builder, ConcurrentRateMeterConfig> {
  public ConcurrentNavigableMapRateMeterTest() {
    super(
        () -> ConcurrentNavigableMapRateMeter.defaultConfig()
            .toBuilder(),
        ConcurrentNavigableMapRateMeter::new);
  }
}