package stinc.male.sandbox.ratmex.meter;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import stinc.male.sandbox.ratmex.meter.RateMeterConfig.Builder;
import stinc.male.sandbox.ratmex.TestTag;

//@Disabled
@Tag(TestTag.CONCURRENCY)
public final class RingBufferRateMeterSequentialTest extends AbstractRateMeterConcurrencyTest<Builder, RateMeterConfig> {
  public RingBufferRateMeterSequentialTest() {
    super(
        () -> new RateMeterConfig.Builder()
            .setTicksCounterSupplier(RingBufferRateMeter.defaultConfig()
                .getTicksCounterSupplier()),
        RingBufferRateMeter::new,
        1);
  }
}