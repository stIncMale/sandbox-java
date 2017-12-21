package stinc.male.sandbox.ratexecutor;

import org.junit.jupiter.api.Tag;
import stinc.male.sandbox.ratexecutor.RateMeterConfig.Builder;
import stinc.male.sandbox.ratexecutor.test.harness.AbstractRateMeterConcurrencyTest;
import stinc.male.test.harness.TestTag;

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