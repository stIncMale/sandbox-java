package stinc.male.sandbox.ratexecutor;

import org.junit.experimental.categories.Category;
import stinc.male.ConcurrencyTest;

@Category(ConcurrencyTest.class)
public final class NavigableMapRateMeterSequentialTest extends AbstractRateMeterConcurrencyTest<RateMeterConfig.Builder, RateMeterConfig> {
  public NavigableMapRateMeterSequentialTest() {
    super(
        RateMeterConfig.Builder::new,
        NavigableMapRateMeter::new,
        NavigableMapRateMeter.defaultConfig().getTicksCounterSupplier(),
        1);
  }
}