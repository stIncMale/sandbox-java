package stinc.male.sandbox.ratmex.meter;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import stinc.male.sandbox.ratmex.meter.RateMeterConfig.Builder;
import stinc.male.sandbox.ratmex.TestTag;

//@Disabled
@Tag(TestTag.CONCURRENCY)
public final class NavigableMapRateMeterSequentialTest extends AbstractRateMeterConcurrencyTest<Builder, RateMeterConfig> {
  public NavigableMapRateMeterSequentialTest() {
    super(
        () -> new RateMeterConfig.Builder()
            .setTicksCounterSupplier(NavigableMapRateMeter.defaultConfig()
                .getTicksCounterSupplier()),
        NavigableMapRateMeter::new,
        1);
  }
}