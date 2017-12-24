package stinc.male.sandbox.ratexecutor;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import stinc.male.sandbox.ratexecutor.ConcurrentRingBufferRateMeterConfig.Builder;
import stinc.male.sandbox.ratexecutor.test.harness.AbstractRateMeterConcurrencyTest;
import stinc.male.test.harness.TestTag;

@Disabled
@Tag(TestTag.CONCURRENCY)
public final class ConcurrentRingBufferRateMeterConcurrentTest
    extends AbstractRateMeterConcurrencyTest<Builder, ConcurrentRingBufferRateMeterConfig> {
  public ConcurrentRingBufferRateMeterConcurrentTest() {
    super(
        () -> {
          final ConcurrentRingBufferRateMeterConfig.Builder result = ConcurrentRingBufferRateMeterConfig.newBuilder()
              .setStrictTick(true);
          result.setTicksCounterSupplier(ConcurrentRingBufferRateMeter.defaultConfig()
              .getTicksCounterSupplier());
          return result;
        },
        ConcurrentRingBufferRateMeter::new,
        Math.max(
            2,
            Runtime.getRuntime()
                .availableProcessors()));
  }
}