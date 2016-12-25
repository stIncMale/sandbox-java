package stinc.male.sandbox.ratexecutor;

import org.junit.experimental.categories.Category;
import stinc.male.ConcurrencyTest;

@Category(ConcurrencyTest.class)
public final class TreeMapRateMeterSerialTest extends AbstractRateMeterParallelTest {
  public TreeMapRateMeterSerialTest() {
    super(
        TreeMapRateMeter::new,
        TreeMapRateMeter.defaultConfig().getTicksCounterSupplier(),
        1);
  }
}