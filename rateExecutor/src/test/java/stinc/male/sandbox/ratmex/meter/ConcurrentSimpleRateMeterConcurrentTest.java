package stinc.male.sandbox.ratmex.meter;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import stinc.male.sandbox.ratmex.meter.RateMeterConfig.Builder;
import stinc.male.sandbox.ratmex.TestTag;

@Disabled
@Tag(TestTag.CONCURRENCY)
public final class ConcurrentSimpleRateMeterConcurrentTest extends AbstractRateMeterConcurrencyTest<Builder, RateMeterConfig> {
  public ConcurrentSimpleRateMeterConcurrentTest() {
    super(
        () -> new RateMeterConfig.Builder()
            .setTicksCounterSupplier(ConcurrentRingBufferRateMeter.defaultConfig()
                .getTicksCounterSupplier()),
        (startNanos, samplesInterval, config) -> new ConcurrentSimpleRateMeter(
            new RingBufferRateMeter(startNanos, samplesInterval, config), new StampedLockStrategy()),
        Math.max(
            2,
            Runtime.getRuntime()
                .availableProcessors()));
  }
}