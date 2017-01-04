package stinc.male.sandbox.ratexecutor;

public final class TreeMapRateMeterTest extends AbstractRateMeterUnitTest<RateMeterConfig.Builder, RateMeterConfig> {
  public TreeMapRateMeterTest() {
    super(
        RateMeterConfig.Builder::new,
        TreeMapRateMeter::new);
  }
}