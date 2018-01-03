package stinc.male.sandbox.ratmex.meter;

import org.junit.jupiter.api.Tag;
import stinc.male.sandbox.ratmex.meter.RateMeterConfig.Builder;
import stinc.male.sandbox.ratmex.TestTag;

@Tag(TestTag.CONCURRENCY)
public final class NavigableMapRateMeterSequentialTest extends AbstractRateMeterConcurrencyTest<Builder, RateMeterConfig> {
  public NavigableMapRateMeterSequentialTest() {
    super(() -> NavigableMapRateMeter.defaultConfig()
            .toBuilder(),
        NavigableMapRateMeter::new, 1);
  }
}