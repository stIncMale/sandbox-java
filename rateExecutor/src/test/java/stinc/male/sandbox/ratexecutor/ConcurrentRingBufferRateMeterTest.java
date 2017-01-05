package stinc.male.sandbox.ratexecutor;

public final class ConcurrentRingBufferRateMeterTest extends AbstractRateMeterUnitTest<ConcurrentRingBufferRateMeterConfig.Builder, ConcurrentRingBufferRateMeterConfig> {
  public ConcurrentRingBufferRateMeterTest() {
    super(
        ConcurrentRingBufferRateMeterConfig.Builder::new,
        ConcurrentRingBufferRateMeter::new);
  }
}