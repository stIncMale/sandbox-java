package stinc.male.sandbox.ratexecutor;

import org.junit.experimental.categories.Category;
import stinc.male.ConcurrencyTest;

@Category(ConcurrencyTest.class)
public final class ConcurrentSkipListMapRateMeterConcurrentTest extends AbstractRateMeterConcurrencyTest<RateMeterConfig.Builder, RateMeterConfig> {
  public ConcurrentSkipListMapRateMeterConcurrentTest() {
    super(
        RateMeterConfig.Builder::new,
        ConcurrentNavigableMapRateMeter::new,
        ConcurrentNavigableMapRateMeter.defaultConfig().getTicksCounterSupplier(),
        Math.max(2, Runtime.getRuntime().availableProcessors()));
  }
}