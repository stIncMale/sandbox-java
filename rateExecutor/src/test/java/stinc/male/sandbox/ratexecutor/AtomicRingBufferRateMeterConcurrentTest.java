package stinc.male.sandbox.ratexecutor;

import org.junit.experimental.categories.Category;
import stinc.male.ConcurrencyTest;

@Category(ConcurrencyTest.class)
public final class AtomicRingBufferRateMeterConcurrentTest extends AbstractRateMeterConcurrencyTest<ConcurrentRingBufferRateMeterConfig.Builder, ConcurrentRingBufferRateMeterConfig> {
  public AtomicRingBufferRateMeterConcurrentTest() {
    super(
        () -> ConcurrentRingBufferRateMeterConfig.newBuilder()
            .setStrictTick(true),
        ConcurrentRingBufferRateMeter::new,
        ConcurrentRingBufferRateMeter.defaultConfig().getTicksCounterSupplier(),
        Math.max(2, Runtime.getRuntime().availableProcessors()));
  }
}