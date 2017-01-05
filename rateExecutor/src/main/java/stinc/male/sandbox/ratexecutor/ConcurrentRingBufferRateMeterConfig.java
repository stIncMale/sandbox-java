package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import java.util.function.Function;
import javax.annotation.concurrent.NotThreadSafe;

public class ConcurrentRingBufferRateMeterConfig extends RateMeterConfig {
  private final boolean strictTick;

  protected ConcurrentRingBufferRateMeterConfig(
      final boolean checkArguments,
      final Function<Long, ? extends TicksCounter> ticksCounterSupplier,
      final Duration timeSensitivity,
      final boolean collectStats,
      final boolean strictTick,
      final int maxTicksCountAttempts) {
    super(
        checkArguments,
        ticksCounterSupplier,
        timeSensitivity,
        collectStats,
        maxTicksCountAttempts);
    this.strictTick = strictTick;
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

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "(checkArguments=" + isCheckArguments()
        + ", ticksCounterSupplier=" + getTicksCounterSupplier()
        + ", timeSensitivity=" + getTimeSensitivity()
        + ", collectStats=" + isCollectStats()
        + ", maxTicksCountAttempts=" + getMaxTicksCountAttempts()
        + ", strictTick=" + strictTick
        + ')';
  }

  @NotThreadSafe
  public static class Builder extends RateMeterConfig.Builder {
    private boolean strictTick;

    protected Builder() {
      strictTick = false;
    }

    protected Builder(final ConcurrentRingBufferRateMeterConfig config) {
      super(config);
      strictTick = config.isStrictTick();
    }

    protected Builder(final RateMeterConfig config) {
      super(config);
      strictTick = false;
    }

    @Override
    public Builder setCheckArguments(final boolean checkArguments) {
      return (Builder)super.setCheckArguments(checkArguments);
    }

    @Override
    public Builder setTicksCounterSupplier(final Function<Long, ? extends TicksCounter> ticksCounterSupplier) {
      return (Builder)super.setTicksCounterSupplier(ticksCounterSupplier);
    }

    @Override
    public Builder setTimeSensitivity(final Duration timeSensitivity) {
      return (Builder)super.setTimeSensitivity(timeSensitivity);
    }

    @Override
    public Builder setCollectStats(final boolean collectStats) {
      return (Builder)super.setCollectStats(collectStats);
    }

    /**
     * @see ConcurrentRingBufferRateMeterConfig#isStrictTick()
     */
    public Builder setStrictTick(final boolean strictTick) {
      this.strictTick = strictTick;
      return this;
    }

    @Override
    public ConcurrentRingBufferRateMeterConfig build() {
      return new ConcurrentRingBufferRateMeterConfig(
          checkArguments,
          ticksCounterSupplier,
          timeSensitivity,
          collectStats,
          strictTick,
          maxTicksCountAttempts);
    }
  }
}