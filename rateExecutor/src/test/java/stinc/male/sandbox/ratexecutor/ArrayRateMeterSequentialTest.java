package stinc.male.sandbox.ratexecutor;

import org.junit.experimental.categories.Category;
import stinc.male.ConcurrencyTest;

@Category(ConcurrencyTest.class)
public final class ArrayRateMeterSequentialTest extends AbstractRateMeterConcurrencyTest<RateMeterConfig.Builder, RateMeterConfig> {
  public ArrayRateMeterSequentialTest() {
    super(
        RateMeterConfig.Builder::new,
        ArrayRateMeter::new,
        ArrayRateMeter.defaultConfig().getTicksCounterSupplier(),
        1);
  }
}