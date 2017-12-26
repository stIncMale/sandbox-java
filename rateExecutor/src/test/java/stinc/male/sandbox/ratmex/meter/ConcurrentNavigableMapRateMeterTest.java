package stinc.male.sandbox.ratmex.meter;

import stinc.male.sandbox.ratmex.meter.RateMeterConfig.Builder;

public final class ConcurrentNavigableMapRateMeterTest extends AbstractRateMeterUnitTest<Builder, RateMeterConfig> {
  public ConcurrentNavigableMapRateMeterTest() {
    super(
        RateMeterConfig.Builder::new,
        ConcurrentNavigableMapRateMeter::new);
  }
}