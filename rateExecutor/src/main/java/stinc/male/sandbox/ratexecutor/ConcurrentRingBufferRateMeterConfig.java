package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import static stinc.male.sandbox.ratexecutor.Preconditions.checkNotNull;

@Immutable
public class ConcurrentRingBufferRateMeterConfig extends RateMeterConfig {
  private final boolean strictTick;
  private final Supplier<? extends WaitStrategy> waitStrategySupplier;
  private final Supplier<? extends LockingStrategy> lockStrategySupplier;

   protected ConcurrentRingBufferRateMeterConfig(
      final boolean checkArguments,
      final Function<Long, ? extends TicksCounter> ticksCounterSupplier,
      final Duration timeSensitivity,
      final boolean collectStats,
      final int maxTicksCountAttempts,
      final int hl,
      final boolean strictTick,
      final Supplier<? extends WaitStrategy> waitStrategySupplier,
      final Supplier<? extends LockingStrategy> lockStrategySupplier) {
    super(
        checkArguments,
        ticksCounterSupplier,
        timeSensitivity,
        collectStats,
        maxTicksCountAttempts,
        hl);
    this.strictTick = strictTick;
    this.waitStrategySupplier = waitStrategySupplier;
    this.lockStrategySupplier = lockStrategySupplier;
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
   * Specifies if {@link ConcurrentRingBufferRateMeter} must guarantee absence of race condition in the {@link RateMeter#tick(long, long)} method.
   * @return {@code false} by default.
   * @see RateMeterStats#failedAccuracyEventsCountForTick()
   */
  public final boolean isStrictTick() {
    return strictTick;
  }

  /**
   * Specifies which {@link WaitStrategy} must be used by {@link ConcurrentRingBufferRateMeter}.
   * @return {@code YieldWaitStrategy::instance} by default.
   */
  public final Supplier<? extends WaitStrategy> getWaitStrategySupplier() {
    return waitStrategySupplier;
  }

  /**
   * Specifies which {@link LockingStrategy} must be used by {@link ConcurrentRingBufferRateMeter}.
   * @return {@code () -> new SpinLockingStrategy(YieldWaitStrategy.instance())} by default.
   */
  public final Supplier<? extends LockingStrategy> getLockStrategySupplier() {
    return lockStrategySupplier;
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
        + ", lockStrategySupplier=" + lockStrategySupplier
        + ')';
  }

  @NotThreadSafe
  public static class Builder extends RateMeterConfig.Builder {
    private boolean strictTick;
    private Supplier<? extends WaitStrategy> waitStrategySupplier;
    private Supplier<? extends LockingStrategy> lockStrategySupplier;

    protected Builder() {
      strictTick = false;
      waitStrategySupplier = YieldWaitStrategy::instance;
      lockStrategySupplier = () -> new SpinLockingStrategy(YieldWaitStrategy.instance());
    }

    protected Builder(final ConcurrentRingBufferRateMeterConfig config) {
      super(config);
      strictTick = config.isStrictTick();
      waitStrategySupplier = config.getWaitStrategySupplier();
    }

    protected Builder(final RateMeterConfig config) {
      super(config);
      strictTick = false;
      waitStrategySupplier = YieldWaitStrategy::instance;
      lockStrategySupplier = () -> new SpinLockingStrategy(YieldWaitStrategy.instance());
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
      checkNotNull(waitStrategySupplier, "waitStrategySupplier");
      this.waitStrategySupplier = waitStrategySupplier;
      return this;
    }

    /**
     * @see ConcurrentRingBufferRateMeterConfig#getLockStrategySupplier()
     */
    public final Builder setLockStrategySupplier(final Supplier<? extends LockingStrategy> lockStrategySupplier) {
      checkNotNull(lockStrategySupplier, "lockStrategySupplier");
      this.lockStrategySupplier = lockStrategySupplier;
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
          waitStrategySupplier,
          lockStrategySupplier);
    }
  }
}