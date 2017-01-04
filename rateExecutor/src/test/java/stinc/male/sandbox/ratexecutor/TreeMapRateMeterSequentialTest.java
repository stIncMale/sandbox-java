package stinc.male.sandbox.ratexecutor;

import org.junit.experimental.categories.Category;
import stinc.male.ConcurrencyTest;

@Category(ConcurrencyTest.class)
public final class TreeMapRateMeterSequentialTest extends AbstractRateMeterConcurrencyTest<RateMeterConfig.Builder, RateMeterConfig> {
  public TreeMapRateMeterSequentialTest() {
    super(
        RateMeterConfig.Builder::new,
        TreeMapRateMeter::new,
        TreeMapRateMeter.defaultConfig().getTicksCounterSupplier(),
        1);
  }
}