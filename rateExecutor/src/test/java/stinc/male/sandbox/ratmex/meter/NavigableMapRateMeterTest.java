package stinc.male.sandbox.ratmex.meter;

import stinc.male.sandbox.ratmex.meter.RateMeterConfig.Builder;

public final class NavigableMapRateMeterTest extends AbstractRateMeterUnitTest<Builder, RateMeterConfig> {
  public NavigableMapRateMeterTest() {
    super(
        RateMeterConfig.Builder::new,
        NavigableMapRateMeter::new);
  }
}