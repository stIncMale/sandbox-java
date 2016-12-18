package stinc.male.sandbox.ratexecutor;

import java.time.Duration;

abstract class AbstractRateMeterTest {
  private final RateMeterCreator rateMeterCreator;

  protected AbstractRateMeterTest(final RateMeterCreator rateMeterCreator) {
    this.rateMeterCreator = rateMeterCreator;
  }

  protected final RateMeterCreator getRateMeterCreator() {
    return rateMeterCreator;
  }

  protected interface RateMeterCreator {
    RateMeter create(long startNanos, Duration samplesInterval, RateMeterConfig config);
  }
}