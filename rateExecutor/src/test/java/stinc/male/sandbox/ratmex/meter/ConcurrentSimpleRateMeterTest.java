package stinc.male.sandbox.ratmex.meter;

import stinc.male.sandbox.ratmex.meter.RateMeterConfig.Builder;

public final class ConcurrentSimpleRateMeterTest extends AbstractRateMeterUnitTest<Builder, RateMeterConfig> {
  public ConcurrentSimpleRateMeterTest() {
    super(
        RateMeterConfig.Builder::new,
        (startNanos, samplesInterval, config) -> new ConcurrentSimpleRateMeter<>(
            new RingBufferRateMeter(startNanos, samplesInterval, config), new StampedLockStrategy()));
  }
}