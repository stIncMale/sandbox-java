package stinc.male.sandbox.ratexecutor;

import org.junit.experimental.categories.Category;
import stinc.male.test.harness.ConcurrencyTest;
import stinc.male.sandbox.ratexecutor.RateMeterConfig.Builder;
import stinc.male.sandbox.ratexecutor.test.harness.AbstractRateMeterConcurrencyTest;

@Category(ConcurrencyTest.class)
public final class NavigableMapRateMeterSequentialTest extends AbstractRateMeterConcurrencyTest<Builder, RateMeterConfig> {
  public NavigableMapRateMeterSequentialTest() {
    super(
        () -> new RateMeterConfig.Builder()
            .setTicksCounterSupplier(NavigableMapRateMeter.defaultConfig().getTicksCounterSupplier()),
        NavigableMapRateMeter::new,
        1);
  }
}