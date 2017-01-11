package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import java.util.function.Function;
import javax.annotation.concurrent.NotThreadSafe;

public class ConcurrentRingBufferRateMeterConfig extends RateMeterConfig {
  private final boolean strictTick;

  ConcurrentRingBufferRateMeterConfig(
      final boolean checkArguments,
      final Function<Long, ? extends TicksCounter> ticksCounterSupplier,
      final Duration timeSensitivity,
      final boolean collectStats,
      final boolean strictTick,
      final int maxTicksCountAttempts,
      final int hl) {
    super(
        checkArguments,
        ticksCounterSupplier,
        timeSensitivity,
        collectStats,
        maxTicksCountAttempts,
        hl);
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
        + ", hl=" + getHl()
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

    /**
     * @see ConcurrentRingBufferRateMeterConfig#isStrictTick()
     */
    public final Builder setStrictTick(final boolean strictTick) {
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
          maxTicksCountAttempts,
          hl);
    }
  }
}