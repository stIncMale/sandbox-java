package stinc.male.sandbox.ratmex.meter;

import org.junit.jupiter.api.Tag;
import stinc.male.sandbox.ratmex.meter.ConcurrentRateMeterConfig.Builder;
import stinc.male.sandbox.ratmex.TestTag;

@Tag(TestTag.CONCURRENCY)
public final class ConcurrentNavigableMapRateMeterConcurrentTest extends AbstractRateMeterConcurrencyTest<Builder, ConcurrentRateMeterConfig> {
  public ConcurrentNavigableMapRateMeterConcurrentTest() {
    super(
        () -> (Builder)ConcurrentNavigableMapRateMeter.defaultConfig()
            .toBuilder()
            .setHistoryLength(2),
        ConcurrentNavigableMapRateMeter::new,
        Math.max(2, Runtime.getRuntime()
            .availableProcessors()));
  }
}