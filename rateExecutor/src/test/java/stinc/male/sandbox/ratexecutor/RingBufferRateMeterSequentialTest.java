package stinc.male.sandbox.ratexecutor;

import org.junit.experimental.categories.Category;
import stinc.male.test.harness.ConcurrencyTest;
import stinc.male.sandbox.ratexecutor.RateMeterConfig.Builder;
import stinc.male.sandbox.ratexecutor.test.harness.AbstractRateMeterConcurrencyTest;

@Category(ConcurrencyTest.class)
public final class RingBufferRateMeterSequentialTest extends AbstractRateMeterConcurrencyTest<Builder, RateMeterConfig> {
  public RingBufferRateMeterSequentialTest() {
    super(
        () -> new RateMeterConfig.Builder()
            .setTicksCounterSupplier(RingBufferRateMeter.defaultConfig().getTicksCounterSupplier()),
        RingBufferRateMeter::new,
        1);
  }
}