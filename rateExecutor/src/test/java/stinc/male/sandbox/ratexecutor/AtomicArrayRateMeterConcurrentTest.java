package stinc.male.sandbox.ratexecutor;

import org.junit.experimental.categories.Category;
import stinc.male.ConcurrencyTest;

@Category(ConcurrencyTest.class)
public final class AtomicArrayRateMeterConcurrentTest extends AbstractRateMeterConcurrentTest {
  public AtomicArrayRateMeterConcurrentTest() {
    super(
        AtomicArrayRateMeter::new,
        AtomicArrayRateMeter.defaultConfig().getTicksCounterSupplier(),
        Math.max(2, Runtime.getRuntime().availableProcessors()));
  }
}