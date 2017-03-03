package stinc.male.sandbox.ratexecutor;

import org.junit.experimental.categories.Category;
import stinc.male.test.harness.ConcurrencyTest;
import stinc.male.sandbox.ratexecutor.RateMeterConfig.Builder;
import stinc.male.sandbox.ratexecutor.test.harness.AbstractRateMeterConcurrencyTest;

@Category(ConcurrencyTest.class)
public final class ConcurrentSimpleRateMeterConcurrentTest extends AbstractRateMeterConcurrencyTest<Builder, RateMeterConfig> {
  public ConcurrentSimpleRateMeterConcurrentTest() {
    super(
        () -> new RateMeterConfig.Builder()
            .setTicksCounterSupplier(ConcurrentRingBufferRateMeter.defaultConfig().getTicksCounterSupplier()),
        (startNanos, samplesInterval, config) -> new ConcurrentSimpleRateMeter(
            new RingBufferRateMeter(startNanos, samplesInterval, config), new StampedLockingStrategy()),
        Math.max(2, Runtime.getRuntime().availableProcessors()));
  }
}