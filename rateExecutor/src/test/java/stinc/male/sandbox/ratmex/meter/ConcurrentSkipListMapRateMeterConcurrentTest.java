package stinc.male.sandbox.ratmex.meter;

import org.junit.jupiter.api.Tag;
import stinc.male.sandbox.ratmex.meter.ConcurrentRateMeterConfig.Builder;
import stinc.male.sandbox.ratmex.TestTag;

@Tag(TestTag.CONCURRENCY)
public final class ConcurrentSkipListMapRateMeterConcurrentTest extends AbstractRateMeterConcurrencyTest<Builder, ConcurrentRateMeterConfig> {
  public ConcurrentSkipListMapRateMeterConcurrentTest() {
    super(
        () -> ConcurrentNavigableMapRateMeter.defaultConfig()
            .toBuilder(),
        ConcurrentNavigableMapRateMeter::new,
        Math.max(2, Runtime.getRuntime()
            .availableProcessors()));
  }
}