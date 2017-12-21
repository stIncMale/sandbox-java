package stinc.male.sandbox.ratexecutor;

import org.junit.jupiter.api.Tag;
import stinc.male.sandbox.ratexecutor.RateMeterConfig.Builder;
import stinc.male.sandbox.ratexecutor.test.harness.AbstractRateMeterConcurrencyTest;
import stinc.male.test.harness.TestTag;

@Tag(TestTag.CONCURRENCY)
public final class RingBufferRateMeterSequentialTest extends AbstractRateMeterConcurrencyTest<Builder, RateMeterConfig> {
  public RingBufferRateMeterSequentialTest() {
    super(
        () -> new RateMeterConfig.Builder()
            .setTicksCounterSupplier(RingBufferRateMeter.defaultConfig()
                .getTicksCounterSupplier()),
        RingBufferRateMeter::new,
        1);
  }
}