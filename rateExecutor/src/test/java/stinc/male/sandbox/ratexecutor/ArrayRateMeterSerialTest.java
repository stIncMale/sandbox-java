package stinc.male.sandbox.ratexecutor;

import org.junit.experimental.categories.Category;
import stinc.male.ConcurrencyTest;

@Category(ConcurrencyTest.class)
public final class ArrayRateMeterSerialTest extends AbstractRateMeterParallelTest {
  public ArrayRateMeterSerialTest() {
    super(ArrayRateMeter::new, 1);
  }
}