package stinc.male.sandbox.ratexecutor;

import org.junit.experimental.categories.Category;
import stinc.male.ConcurrencyTest;

@Category(ConcurrencyTest.class)
public final class ArrayRateMeterSerialTest extends AbstractRateMeterConcurrentTest {
  public ArrayRateMeterSerialTest() {
    super(
        ArrayRateMeter::new,
        ArrayRateMeter.defaultConfig().getTicksCounterSupplier(),
        1);
  }
}