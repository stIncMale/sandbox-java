package stinc.male.sandbox.ratexecutor;

import org.junit.experimental.categories.Category;
import stinc.male.test.harness.ConcurrencyTest;
import stinc.male.sandbox.ratexecutor.RateMeterConfig.Builder;
import stinc.male.sandbox.ratexecutor.test.harness.AbstractRateMeterConcurrencyTest;

@Category(ConcurrencyTest.class)
public final class ConcurrentSkipListMapRateMeterConcurrentTest extends AbstractRateMeterConcurrencyTest<Builder, RateMeterConfig> {
  public ConcurrentSkipListMapRateMeterConcurrentTest() {
    super(
        () -> new RateMeterConfig.Builder()
            .setTicksCounterSupplier(ConcurrentNavigableMapRateMeter.defaultConfig().getTicksCounterSupplier()),
        ConcurrentNavigableMapRateMeter::new,
        Math.max(2, Runtime.getRuntime().availableProcessors()));
  }
}