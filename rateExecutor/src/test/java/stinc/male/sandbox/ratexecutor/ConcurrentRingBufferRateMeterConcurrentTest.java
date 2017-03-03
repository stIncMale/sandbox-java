package stinc.male.sandbox.ratexecutor;

import org.junit.experimental.categories.Category;
import stinc.male.test.harness.ConcurrencyTest;
import stinc.male.sandbox.ratexecutor.ConcurrentRingBufferRateMeterConfig.Builder;
import stinc.male.sandbox.ratexecutor.test.harness.AbstractRateMeterConcurrencyTest;

@Category(ConcurrencyTest.class)
public final class ConcurrentRingBufferRateMeterConcurrentTest extends AbstractRateMeterConcurrencyTest<Builder, ConcurrentRingBufferRateMeterConfig> {
  public ConcurrentRingBufferRateMeterConcurrentTest() {
    super(
        () -> {
          final ConcurrentRingBufferRateMeterConfig.Builder result = ConcurrentRingBufferRateMeterConfig.newBuilder()
            .setStrictTick(true);
          result.setTicksCounterSupplier(ConcurrentRingBufferRateMeter.defaultConfig().getTicksCounterSupplier());
          return result;
        },
        ConcurrentRingBufferRateMeter::new,
        Math.max(2, Runtime.getRuntime().availableProcessors()));
  }
}