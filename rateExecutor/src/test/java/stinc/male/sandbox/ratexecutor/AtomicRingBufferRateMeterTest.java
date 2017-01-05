package stinc.male.sandbox.ratexecutor;

public final class AtomicRingBufferRateMeterTest extends AbstractRateMeterUnitTest<ConcurrentRingBufferRateMeterConfig.Builder, ConcurrentRingBufferRateMeterConfig> {
  public AtomicRingBufferRateMeterTest() {
    super(
        ConcurrentRingBufferRateMeterConfig.Builder::new,
        ConcurrentRingBufferRateMeter::new);
  }
}