package stinc.male.sandbox.ratexecutor;

import org.junit.jupiter.api.Tag;
import stinc.male.sandbox.ratexecutor.RateMeterConfig.Builder;
import stinc.male.sandbox.ratexecutor.test.harness.AbstractRateMeterConcurrencyTest;
import stinc.male.test.harness.TestTag;

@Tag(TestTag.CONCURRENCY)
public final class ConcurrentSimpleRateMeterConcurrentTest extends AbstractRateMeterConcurrencyTest<Builder, RateMeterConfig> {
  public ConcurrentSimpleRateMeterConcurrentTest() {
    super(
        () -> new RateMeterConfig.Builder()
            .setTicksCounterSupplier(ConcurrentRingBufferRateMeter.defaultConfig()
                .getTicksCounterSupplier()),
        (startNanos, samplesInterval, config) -> new ConcurrentSimpleRateMeter(
            new RingBufferRateMeter(startNanos, samplesInterval, config), new StampedLockingStrategy()),
        Math.max(
            2,
            Runtime.getRuntime()
                .availableProcessors()));
  }
}