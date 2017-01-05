package stinc.male.sandbox.ratexecutor;

public final class NavigableMapRateMeterTest extends AbstractRateMeterUnitTest<RateMeterConfig.Builder, RateMeterConfig> {
  public NavigableMapRateMeterTest() {
    super(
        RateMeterConfig.Builder::new,
        NavigableMapRateMeter::new);
  }
}