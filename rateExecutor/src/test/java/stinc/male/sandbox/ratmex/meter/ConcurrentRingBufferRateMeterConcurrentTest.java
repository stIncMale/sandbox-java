package stinc.male.sandbox.ratmex.meter;

import org.junit.jupiter.api.Tag;
import stinc.male.sandbox.ratmex.meter.ConcurrentRateMeterConfig.Builder;
import stinc.male.sandbox.ratmex.TestTag;

@Tag(TestTag.CONCURRENCY)
public final class ConcurrentRingBufferRateMeterConcurrentTest extends AbstractRateMeterConcurrencyTest<Builder, ConcurrentRateMeterConfig> {
  public ConcurrentRingBufferRateMeterConcurrentTest() {
    super(
        () -> (Builder)ConcurrentRingBufferRateMeter.defaultConfig()
            .toBuilder()
            .setHistoryLength(2),
        ConcurrentRingBufferRateMeter::new,
        Math.max(2, Runtime.getRuntime()
            .availableProcessors()));
  }
}