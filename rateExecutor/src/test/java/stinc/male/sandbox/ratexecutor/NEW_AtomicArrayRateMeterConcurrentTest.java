package stinc.male.sandbox.ratexecutor;

import org.junit.experimental.categories.Category;
import stinc.male.ConcurrencyTest;

@Category(ConcurrencyTest.class)
public final class NEW_AtomicArrayRateMeterConcurrentTest extends AbstractRateMeterConcurrentTest {
  public NEW_AtomicArrayRateMeterConcurrentTest() {
    super(
        NEW_AtomicArrayRateMeter::new,
        NEW_AtomicArrayRateMeter.defaultConfig().getTicksCounterSupplier(),
        Math.max(2, Runtime.getRuntime().availableProcessors()));
  }
}