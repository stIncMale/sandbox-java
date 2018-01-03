package stinc.male.sandbox.ratmex.meter;

import org.junit.jupiter.api.Tag;
import stinc.male.sandbox.ratmex.meter.RateMeterConfig.Builder;
import stinc.male.sandbox.ratmex.TestTag;

@Tag(TestTag.CONCURRENCY)
public final class RingBufferRateMeterSequentialTest extends AbstractRateMeterConcurrencyTest<Builder, RateMeterConfig> {
  public RingBufferRateMeterSequentialTest() {
    super(
        () -> RingBufferRateMeter.defaultConfig()
            .toBuilder(),
        RingBufferRateMeter::new, 1);
  }
}