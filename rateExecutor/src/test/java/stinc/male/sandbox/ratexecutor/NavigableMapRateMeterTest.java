package stinc.male.sandbox.ratexecutor;

import stinc.male.sandbox.ratexecutor.RateMeterConfig.Builder;
import stinc.male.sandbox.ratexecutor.test.harness.AbstractRateMeterUnitTest;

public final class NavigableMapRateMeterTest extends AbstractRateMeterUnitTest<Builder, RateMeterConfig> {
  public NavigableMapRateMeterTest() {
    super(
        RateMeterConfig.Builder::new,
        NavigableMapRateMeter::new);
  }
}