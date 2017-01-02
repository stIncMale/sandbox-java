package stinc.male.sandbox.ratexecutor;

import org.junit.experimental.categories.Category;
import stinc.male.ConcurrencyTest;

@Category(ConcurrencyTest.class)
public final class ConcurrentSkipListMapRateMeterConcurrentTest extends AbstractRateMeterConcurrentTest {
  public ConcurrentSkipListMapRateMeterConcurrentTest() {
    super(
        ConcurrentSkipListMapRateMeter::new,
        ConcurrentSkipListMapRateMeter.defaultConfig().getTicksCounterSupplier(),
        Math.max(2, Runtime.getRuntime().availableProcessors()));
  }
}