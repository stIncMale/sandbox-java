package stinc.male.sandbox.ratmex.meter;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import stinc.male.sandbox.ratmex.meter.ConcurrentRingBufferRateMeterConfig.Builder;
import stinc.male.sandbox.ratmex.TestTag;

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