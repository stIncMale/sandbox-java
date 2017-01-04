package stinc.male.sandbox.ratexecutor;

public final class AtomicArrayRateMeterTest extends AbstractRateMeterUnitTest<ConcurrentRingBufferRateMeterConfig.Builder, ConcurrentRingBufferRateMeterConfig> {
  public AtomicArrayRateMeterTest() {
    super(
        ConcurrentRingBufferRateMeterConfig.Builder::new,
        AtomicArrayRateMeter::new);
  }
}