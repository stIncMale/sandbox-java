package stinc.male.sandbox.ratexecutor.test.harness;

import java.time.Duration;
import java.util.function.Supplier;
import stinc.male.sandbox.ratexecutor.RateMeter;
import stinc.male.sandbox.ratexecutor.RateMeterConfig;
import stinc.male.sandbox.ratexecutor.RateMeterConfig.Builder;

abstract class AbstractRateMeterTest<B extends Builder, C extends RateMeterConfig> {
  private final Supplier<B> rateMeterConfigBuilderSupplier;
  private final RateMeterCreator<C> rateMeterCreator;

  protected AbstractRateMeterTest(
      final Supplier<B> rateMeterConfigBuilderSupplier,
      final RateMeterCreator<C> rateMeterCreator) {
    this.rateMeterConfigBuilderSupplier = rateMeterConfigBuilderSupplier;
    this.rateMeterCreator = rateMeterCreator;
  }

  protected final Supplier<B> getRateMeterConfigBuilderSupplier() {
    return rateMeterConfigBuilderSupplier;
  }

  protected final RateMeterCreator<C> getRateMeterCreator() {
    return rateMeterCreator;
  }

  protected interface RateMeterCreator<C> {
    RateMeter create(long startNanos, Duration samplesInterval, C config);
  }
}