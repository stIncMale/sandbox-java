package stinc.male.sandbox.ratexecutor;

public final class ArrayRateMeterTest extends AbstractRateMeterUnitTest<RateMeterConfig.Builder, RateMeterConfig> {
  public ArrayRateMeterTest() {
    super(
        RateMeterConfig.Builder::new,
        ArrayRateMeter::new);
  }
}