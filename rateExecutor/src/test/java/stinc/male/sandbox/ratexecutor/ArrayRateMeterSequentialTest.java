package stinc.male.sandbox.ratexecutor;

import org.junit.experimental.categories.Category;
import stinc.male.ConcurrencyTest;

@Category(ConcurrencyTest.class)
public final class ArrayRateMeterSequentialTest extends AbstractRateMeterConcurrentTest {
  public ArrayRateMeterSequentialTest() {
    super(
        ArrayRateMeter::new,
        ArrayRateMeter.defaultConfig().getTicksCounterSupplier(),
        1);
  }
}