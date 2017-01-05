package stinc.male.sandbox.ratexecutor;

import org.junit.experimental.categories.Category;
import stinc.male.ConcurrencyTest;

@Category(ConcurrencyTest.class)
public final class RingBufferRateMeterSequentialTest extends AbstractRateMeterConcurrencyTest<RateMeterConfig.Builder, RateMeterConfig> {
  public RingBufferRateMeterSequentialTest() {
    super(
        RateMeterConfig.Builder::new,
        RingBufferRateMeter::new,
        RingBufferRateMeter.defaultConfig().getTicksCounterSupplier(),
        1);
  }
}