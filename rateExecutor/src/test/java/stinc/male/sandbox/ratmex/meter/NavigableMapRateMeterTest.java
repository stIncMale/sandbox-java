package stinc.male.sandbox.ratmex.meter;

import stinc.male.sandbox.ratmex.meter.RateMeterConfig.Builder;

public final class NavigableMapRateMeterTest extends AbstractRateMeterUnitTest<Builder, RateMeterConfig> {
  public NavigableMapRateMeterTest() {
    super(
        () -> NavigableMapRateMeter.defaultConfig()
            .toBuilder()
            .setHistoryLength(2),
        NavigableMapRateMeter::new);
  }
}