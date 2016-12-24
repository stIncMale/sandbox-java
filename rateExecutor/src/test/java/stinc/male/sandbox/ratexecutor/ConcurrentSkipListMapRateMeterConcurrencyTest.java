package stinc.male.sandbox.ratexecutor;

import org.junit.experimental.categories.Category;
import stinc.male.ConcurrencyTest;

@Category(ConcurrencyTest.class)
public final class ConcurrentSkipListMapRateMeterConcurrencyTest extends AbstractRateMeterParallelTest {
  public ConcurrentSkipListMapRateMeterConcurrencyTest() {
    super(ConcurrentSkipListMapRateMeter::new, 10);
  }
}