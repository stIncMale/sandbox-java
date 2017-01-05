package stinc.male.sandbox.ratexecutor;

public final class ConcurrentNavigableMapRateMeterTest extends AbstractRateMeterUnitTest<RateMeterConfig.Builder, RateMeterConfig> {
  public ConcurrentNavigableMapRateMeterTest() {
    super(
        RateMeterConfig.Builder::new,
        ConcurrentNavigableMapRateMeter::new);
  }
}