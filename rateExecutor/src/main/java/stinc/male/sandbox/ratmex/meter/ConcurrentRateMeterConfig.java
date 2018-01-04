package stinc.male.sandbox.ratmex.meter;

import java.time.Duration;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import static stinc.male.sandbox.ratmex.internal.util.Preconditions.checkArgument;
import static stinc.male.sandbox.ratmex.internal.util.Preconditions.checkNotNull;

/**
 * A configuration that can be used to create concurrent implementations of {@link AbstractRateMeter},
 * e.g. {@link ConcurrentNavigableMapRateMeter}, {@link ConcurrentRingBufferRateMeter}.
 */
@Immutable
public class ConcurrentRateMeterConfig extends RateMeterConfig {
  private final int maxTicksCountAttempts;
  private final boolean strictTick;
  private final boolean collectStats;
  private final Supplier<? extends WaitStrategy> waitStrategySupplier;
  private final Supplier<? extends LockStrategy> lockStrategySupplier;

  protected ConcurrentRateMeterConfig(
      final Function<Long, ? extends TicksCounter> ticksCounterSupplier,
      @Nullable final Duration timeSensitivity,
      final int historyLength,
      final int maxTicksCountAttempts,
      final boolean strictTick,
      final boolean collectStats,
      final Supplier<? extends WaitStrategy> waitStrategySupplier,
      final Supplier<? extends LockStrategy> lockStrategySupplier) {
    super(
        ticksCounterSupplier,
        timeSensitivity,
        historyLength);
    checkArgument(maxTicksCountAttempts > 0, "maxTicksCountAttempts", "Must be positive");
    checkNotNull(waitStrategySupplier, "waitStrategySupplier");
    checkNotNull(lockStrategySupplier, "lockStrategySupplier");
    this.maxTicksCountAttempts = maxTicksCountAttempts;
    this.strictTick = strictTick;
    this.collectStats = collectStats;
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
   * Specifies the desired maximum number of attempts to calculate the number of ticks (see {@link RateMeter#ticksCount()} for example).
   * Note that this is just a hint, so an implementation may choose to do more attempts, but the number of attempts must be finite.
   * <p>
   * <b>The reasoning behind this hint</b><br>
   * Implementations may allow a race condition (for performance reasons) while counting ticks.
   * When running out of the number of attempts such implementations may choose to fall over to an approach that excludes the race
   * and allows to eventually count the ticks.
   *
   * @return 6 by default.
   */
  public final int getMaxTicksCountAttempts() {
    return maxTicksCountAttempts;
  }

  /**
   * Specifies if {@link ConcurrentRingBufferRateMeter} (or any other {@link RateMeter} which explicitly says it does this)
   * must guarantee a strict behavior of {@link RateMeter#tick(long, long)} method.
   *
   * @return true by default.
   *
   * @see ConcurrentRateMeterStats#failedAccuracyEventsCountForTick()
   */
  public final boolean isStrictTick() {
    return strictTick;
  }

  /**
   * This configuration parameter specifies if a {@link RateMeter} which can collect {@link RateMeter#stats() stats} must do so.
   *
   * @return false by default.
   */
  public final boolean isCollectStats() {
    return collectStats;
  }

  /**
   * Specifies which {@link WaitStrategy} must be used by a thread-safe implementation {@link RateMeter} (if it at all uses it).
   *
   * @return {@link ParkWaitStrategy}{@code ::}{@link ParkWaitStrategy#defaultInstance() defaultInstance} by default.
   */
  public final Supplier<? extends WaitStrategy> getWaitStrategySupplier() {
    return waitStrategySupplier;
  }

  /**
   * Specifies which {@link LockStrategy} must be used by a thread-safe implementation {@link RateMeter} (if it at all uses it).
   *
   * @return {@link StampedLockStrategy}{@code ::}{@link StampedLockStrategy#StampedLockStrategy new} by default.
   */
  public final Supplier<? extends LockStrategy> getLockStrategySupplier() {
    return lockStrategySupplier;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "{ticksCounterSupplier=" + getTicksCounterSupplier()
        + ", timeSensitivity=" + getTimeSensitivity().orElse(null)
        + ", historyLength=" + getHistoryLength()
        + ", maxTicksCountAttempts=" + maxTicksCountAttempts
        + ", strictTick=" + strictTick
        + ", collectStats=" + collectStats
        + ", waitStrategySupplier=" + waitStrategySupplier
        + ", lockStrategySupplier=" + lockStrategySupplier
        + '}';
  }

  @NotThreadSafe
  public static class Builder extends RateMeterConfig.Builder {
    protected int maxTicksCountAttempts;
    protected boolean strictTick;
    protected boolean collectStats;
    protected Supplier<? extends WaitStrategy> waitStrategySupplier;
    protected Supplier<? extends LockStrategy> lockStrategySupplier;

    protected Builder() {
      maxTicksCountAttempts = 6;
      strictTick = true;
      collectStats = false;
      waitStrategySupplier = ParkWaitStrategy::defaultInstance;
      lockStrategySupplier = StampedLockStrategy::new;
    }

    /**
     * @param config Must not be null.
     */
    protected Builder(final ConcurrentRateMeterConfig config) {
      super(config);
      maxTicksCountAttempts = config.getMaxTicksCountAttempts();
      strictTick = config.isStrictTick();
      collectStats = config.isCollectStats();
      waitStrategySupplier = config.getWaitStrategySupplier();
      lockStrategySupplier = config.getLockStrategySupplier();
    }

    /**
     * @param config Must not be null.
     */
    protected Builder(final RateMeterConfig config) {
      this();
      checkNotNull(config, "config");
      set(config);
    }

    /**
     * @param config Must not be null.
     */
    public final Builder set(final RateMeterConfig config) {
      checkNotNull(config, "config");
      ticksCounterSupplier = config.getTicksCounterSupplier();
      historyLength = config.getHistoryLength();
      timeSensitivity = config.getTimeSensitivity()
          .orElse(null);
      return this;
    }

    /**
     * @param maxTicksCountAttempts Must be positive.
     *
     * @see ConcurrentRateMeterConfig#getMaxTicksCountAttempts()
     */
    public final RateMeterConfig.Builder setMaxTicksCountAttempts(final int maxTicksCountAttempts) {
      checkArgument(maxTicksCountAttempts > 0, "maxTicksCountAttempts", "Must be positive");
      this.maxTicksCountAttempts = maxTicksCountAttempts;
      return this;
    }

    /**
     * @see ConcurrentRateMeterConfig#isStrictTick()
     */
    public final Builder setStrictTick(final boolean strictTick) {
      this.strictTick = strictTick;
      return this;
    }

    /**
     * @see ConcurrentRateMeterConfig#isCollectStats()
     */
    public final Builder setCollectStats(final boolean collectStats) {
      this.collectStats = collectStats;
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
    public ConcurrentRateMeterConfig build() {
      return new ConcurrentRateMeterConfig(
          ticksCounterSupplier,
          timeSensitivity,
          historyLength,
          maxTicksCountAttempts,
          strictTick,
          collectStats,
          waitStrategySupplier,
          lockStrategySupplier);
    }
  }
}