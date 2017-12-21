package stinc.male.sandbox.ratexecutor;

import org.junit.jupiter.api.Tag;
import stinc.male.sandbox.ratexecutor.RateMeterConfig.Builder;
import stinc.male.sandbox.ratexecutor.test.harness.AbstractRateMeterConcurrencyTest;
import stinc.male.test.harness.TestTag;

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