package stinc.male.sandbox.ratexecutor;

import org.junit.experimental.categories.Category;
import stinc.male.ConcurrencyTest;

@Category(ConcurrencyTest.class)
public final class TreeMapRateMeterSequentialTest extends AbstractRateMeterConcurrentTest {
  public TreeMapRateMeterSequentialTest() {
    super(
        TreeMapRateMeter::new,
        TreeMapRateMeter.defaultConfig().getTicksCounterSupplier(),
        1);
  }
}