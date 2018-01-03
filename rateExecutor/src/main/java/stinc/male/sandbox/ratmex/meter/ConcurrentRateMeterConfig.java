package stinc.male.sandbox.ratmex.meter;

import java.time.Duration;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import static stinc.male.sandbox.ratmex.internal.util.Preconditions.checkNotNull;

@Immutable
public final class ConcurrentRateMeterConfig extends RateMeterConfig {//TODO check arguments
  private final boolean strictTick;
  private final boolean collectStats;
  private final Supplier<? extends WaitStrategy> waitStrategySupplier;
  private final Supplier<? extends LockStrategy> lockStrategySupplier;

  ConcurrentRateMeterConfig(
      final Function<Long, ? extends TicksCounter> ticksCounterSupplier,
      @Nullable final Duration timeSensitivity,
      final int maxTicksCountAttempts,
      final int historyLength,
      final boolean strictTick,
      final boolean collectStats,
      final Supplier<? extends WaitStrategy> waitStrategySupplier,
      final Supplier<? extends LockStrategy> lockStrategySupplier) {
    super(
        ticksCounterSupplier,
        timeSensitivity,
        maxTicksCountAttempts,
        historyLength);
    this.strictTick = strictTick;
    this.collectStats = collectStats;
    this.waitStrategySupplier = waitStrategySupplier;
    this.lockStrategySupplier = lockStrategySupplier;
  }

  public static final Builder newBuilder() {
    return new Builder();
  }

  public static final Builder newBuilder(final RateMeterConfig config) {
    return new Builder(config);
  }

  @Override
  public final Builder toBuilder() {
    return new Builder(this);
  }

  /**
   * Specifies if {@link ConcurrentRingBufferRateMeter} must guarantee absence of race conditions in the {@link RateMeter#tick(long, long)} method.
   *
   * @return true by default.
   *
   * @see ConcurrentRingBufferRateMeterStats#failedAccuracyEventsCountForTick()
   */
  public final boolean isStrictTick() {
    return strictTick;
  }

  /**
   * This configuration parameter specifies if {@link ConcurrentRingBufferRateMeter} must collect {@link ConcurrentRingBufferRateMeter#stats() stats}.
   *
   * @return false by default.
   * If {@link #isStrictTick()} is true, then returns false.
   */
  public final boolean isCollectStats() {
    return !isStrictTick() && collectStats;
  }

  /**
   * Specifies which {@link WaitStrategy} must be used by {@link ConcurrentRingBufferRateMeter}.
   *
   * @return {@link ParkWaitStrategy}{@code ::}{@link ParkWaitStrategy#instance() instance} by default.
   */
  public final Supplier<? extends WaitStrategy> getWaitStrategySupplier() {
    return waitStrategySupplier;
  }

  /**
   * Specifies which {@link LockStrategy} must be used by {@link ConcurrentRingBufferRateMeter}.
   *
   * @return {@code () -> new SpinLockStrategy(YieldWaitStrategy.instance())} by default.
   */
  public final Supplier<? extends LockStrategy> getLockStrategySupplier() {
    return lockStrategySupplier;
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName()
        + "{ticksCounterSupplier=" + getTicksCounterSupplier()
        + ", timeSensitivity=" + getTimeSensitivity().orElse(null)
        + ", collectStats=" + isCollectStats()
        + ", maxTicksCountAttempts=" + getMaxTicksCountAttempts()
        + ", historyLength=" + getHistoryLength()
        + ", strictTick=" + strictTick
        + ", collectStats=" + isCollectStats()
        + ", waitStrategySupplier=" + waitStrategySupplier
        + ", lockStrategySupplier=" + lockStrategySupplier
        + '}';
  }

  @NotThreadSafe
  public static final class Builder extends RateMeterConfig.Builder {
    private boolean strictTick;
    private boolean collectStats;
    private Supplier<? extends WaitStrategy> waitStrategySupplier;
    private Supplier<? extends LockStrategy> lockStrategySupplier;

    Builder() {
      strictTick = true;
      collectStats = false;
      waitStrategySupplier = ParkWaitStrategy::instance;
      lockStrategySupplier = () -> new SpinLockStrategy(YieldWaitStrategy.instance());
    }

    Builder(final ConcurrentRateMeterConfig config) {
      super(config);
      strictTick = config.isStrictTick();
      collectStats = config.isCollectStats();
      waitStrategySupplier = config.getWaitStrategySupplier();
      lockStrategySupplier = config.getLockStrategySupplier();
    }

    Builder(final RateMeterConfig config) {
      this();
      set(config);
    }

    public final Builder set(final RateMeterConfig config) {
      ticksCounterSupplier = config.getTicksCounterSupplier();
      maxTicksCountAttempts = config.getMaxTicksCountAttempts();
      historyLength = config.getHistoryLength();
      timeSensitivity = config.getTimeSensitivity()
          .orElse(null);
      return this;
    }

    /**
     * Sets {@link ConcurrentRateMeterConfig#isCollectStats() collect stats} to false if {@code strictTick} is true.
     *
     * @see ConcurrentRateMeterConfig#isStrictTick()
     */
    public final Builder setStrictTick(final boolean strictTick) {
      this.strictTick = strictTick;
      if (strictTick) {
        collectStats = false;
      }
      return this;
    }

    /**
     * The value of {@code collectStats} is ignored if {@link ConcurrentRateMeterConfig#isStrictTick() strict tick} is enabled.
     *
     * @see ConcurrentRateMeterConfig#isCollectStats()
     */
    public final Builder setCollectStats(final boolean collectStats) {
      if (!strictTick) {
        this.collectStats = collectStats;
      }
      return this;
    }

    /**
     * @see ConcurrentRateMeterConfig#getWaitStrategySupplier()
     */
    public final Builder setWaitStrategySupplier(final Supplier<? extends WaitStrategy> waitStrategySupplier) {
      checkNotNull(waitStrategySupplier, "waitStrategySupplier");
      this.waitStrategySupplier = waitStrategySupplier;
      return this;
    }

    /**
     * @see ConcurrentRateMeterConfig#getLockStrategySupplier()
     */
    public final Builder setLockStrategySupplier(final Supplier<? extends LockStrategy> lockStrategySupplier) {
      checkNotNull(lockStrategySupplier, "lockStrategySupplier");
      this.lockStrategySupplier = lockStrategySupplier;
      return this;
    }

    @Override
    public final ConcurrentRateMeterConfig build() {
      return new ConcurrentRateMeterConfig(
          ticksCounterSupplier,
          timeSensitivity,
          maxTicksCountAttempts,
          historyLength,
          strictTick,
          collectStats,
          waitStrategySupplier,
          lockStrategySupplier);
    }
  }
}