package stinc.male.sandbox.ratexecutor;

public final class RingBufferRateMeterTest extends AbstractRateMeterUnitTest<RateMeterConfig.Builder, RateMeterConfig> {
  public RingBufferRateMeterTest() {
    super(
        RateMeterConfig.Builder::new,
        RingBufferRateMeter::new);
  }
}