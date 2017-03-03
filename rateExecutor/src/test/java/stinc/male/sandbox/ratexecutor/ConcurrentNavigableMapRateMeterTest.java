package stinc.male.sandbox.ratexecutor;

import stinc.male.sandbox.ratexecutor.RateMeterConfig.Builder;
import stinc.male.sandbox.ratexecutor.test.harness.AbstractRateMeterUnitTest;

public final class ConcurrentNavigableMapRateMeterTest extends AbstractRateMeterUnitTest<Builder, RateMeterConfig> {
  public ConcurrentNavigableMapRateMeterTest() {
    super(
        RateMeterConfig.Builder::new,
        ConcurrentNavigableMapRateMeter::new);
  }
}