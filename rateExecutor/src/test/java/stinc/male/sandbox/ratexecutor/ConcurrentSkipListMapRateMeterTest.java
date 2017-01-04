package stinc.male.sandbox.ratexecutor;

public final class ConcurrentSkipListMapRateMeterTest extends AbstractRateMeterUnitTest<RateMeterConfig.Builder, RateMeterConfig> {
  public ConcurrentSkipListMapRateMeterTest() {
    super(
        RateMeterConfig.Builder::new,
        ConcurrentSkipListMapRateMeter::new);
  }
}