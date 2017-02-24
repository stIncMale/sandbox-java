package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.concurrent.NotThreadSafe;

public class ConcurrentRingBufferRateMeterConfig extends RateMeterConfig {
  private final boolean strictTick;
  private final Supplier<? extends WaitStrategy> waitStrategySupplier;

  ConcurrentRingBufferRateMeterConfig(
      final boolean checkArguments,
      final Function<Long, ? extends TicksCounter> ticksCounterSupplier,
      final Duration timeSensitivity,
      final boolean collectStats,
      final int maxTicksCountAttempts,
      final int hl,
      final boolean strictTick,
      final Supplier<? extends WaitStrategy> waitStrategySupplier) {
    super(
        checkArguments,
        ticksCounterSupplier,
        timeSensitivity,
        collectStats,
        maxTicksCountAttempts,
        hl);
    this.strictTick = strictTick;
    this.waitStrategySupplier = waitStrategySupplier;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static Builder newBuilder(final RateMeterConfig config) {
    return new Builder(config);
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  /**
   * Specifies if {@link AbstractRateMeter} must guarantee absence of race condition in the {@link RateMeter#tick(long, long)} method.
   * @return {@code false} by default.
   */
  public final boolean isStrictTick() {
    return strictTick;
  }

  /**
   * Specifies which {@link WaitStrategy} must be used by {@link RateMeter}.
   * @return {@code YieldWaitStrategy::new} by default.
   */
  public final Supplier<? extends WaitStrategy> getWaitStrategySupplier() {
    return waitStrategySupplier;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "(checkArguments=" + isCheckArguments()
        + ", ticksCounterSupplier=" + getTicksCounterSupplier()
        + ", timeSensitivity=" + getTimeSensitivity()
        + ", collectStats=" + isCollectStats()
        + ", maxTicksCountAttempts=" + getMaxTicksCountAttempts()
        + ", hl=" + getHl()
        + ", strictTick=" + strictTick
        + ", waitStrategySupplier=" + waitStrategySupplier
        + ')';
  }

  @NotThreadSafe
  public static class Builder extends RateMeterConfig.Builder {
    private boolean strictTick;
    private Supplier<? extends WaitStrategy> waitStrategySupplier;

    protected Builder() {
      strictTick = false;
      waitStrategySupplier = YieldWaitStrategy::new;
    }

    protected Builder(final ConcurrentRingBufferRateMeterConfig config) {
      super(config);
      strictTick = config.isStrictTick();
      waitStrategySupplier = config.getWaitStrategySupplier();
    }

    protected Builder(final RateMeterConfig config) {
      super(config);
      strictTick = false;
      waitStrategySupplier = YieldWaitStrategy::new;
    }

    /**
     * @see ConcurrentRingBufferRateMeterConfig#isStrictTick()
     */
    public final Builder setStrictTick(final boolean strictTick) {
      this.strictTick = strictTick;
      return this;
    }

    /**
     * @see ConcurrentRingBufferRateMeterConfig#getWaitStrategySupplier()
     */
    public final Builder setWaitStrategySupplier(final Supplier<? extends WaitStrategy> waitStrategySupplier) {
      this.waitStrategySupplier = waitStrategySupplier;
      return this;
    }

    @Override
    public ConcurrentRingBufferRateMeterConfig build() {
      return new ConcurrentRingBufferRateMeterConfig(
          checkArguments,
          ticksCounterSupplier,
          timeSensitivity,
          collectStats,
          maxTicksCountAttempts,
          hl,
          strictTick,
          waitStrategySupplier);
    }
  }
}