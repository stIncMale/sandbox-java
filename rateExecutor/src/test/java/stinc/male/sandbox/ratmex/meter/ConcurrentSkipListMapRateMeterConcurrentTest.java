package stinc.male.sandbox.ratmex.meter;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import stinc.male.sandbox.ratmex.meter.RateMeterConfig.Builder;
import stinc.male.sandbox.ratmex.TestTag;

//@Disabled
@Tag(TestTag.CONCURRENCY)
public final class ConcurrentSkipListMapRateMeterConcurrentTest extends AbstractRateMeterConcurrencyTest<Builder, RateMeterConfig> {
  public ConcurrentSkipListMapRateMeterConcurrentTest() {
    super(
        () -> new RateMeterConfig.Builder()
            .setTicksCounterSupplier(ConcurrentNavigableMapRateMeter.defaultConfig()
                .getTicksCounterSupplier()),
        ConcurrentNavigableMapRateMeter::new,
        Math.max(
            2,
            Runtime.getRuntime()
                .availableProcessors()));
  }
}