package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import java.util.function.Function;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import static stinc.male.sandbox.ratexecutor.util.internal.Preconditions.checkArgument;
import static stinc.male.sandbox.ratexecutor.util.internal.Preconditions.checkNotNull;

/**
 * The {@code @}{@link Immutable} here only guarantees that the {@code get...} methods
 * always behave as methods of immutable class.
 */
@Immutable
public class RateMeterConfig {
  private final Function<Long, ? extends TicksCounter> ticksCounterSupplier;
  private final Duration timeSensitivity;
  private final boolean collectStats;
  private final int maxTicksCountAttempts;
  private final int hl;

  protected RateMeterConfig(
      final Function<Long, ? extends TicksCounter> ticksCounterSupplier,
      final Duration timeSensitivity,
      final boolean collectStats,
      final int maxTicksCountAttempts,
      final int hl) {
    this.ticksCounterSupplier = ticksCounterSupplier;
    this.timeSensitivity = timeSensitivity;
    this.collectStats = collectStats;
    this.maxTicksCountAttempts = maxTicksCountAttempts;
    this.hl = hl;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public Builder toBuilder() {
    return new Builder(this);
  }

  /**
   * Specifies a supplier which MUST be used by {@link AbstractRateMeter} to create ticks counters.
   * Note that if {@link AbstractRateMeter} is used concurrently
   * then supplier MUST provide a thread-safe implementation of {@link TicksCounter}.
   *
   * @return {@code LongAdderTicksCounter::new} by default.
   */
  public final Function<Long, ? extends TicksCounter> getTicksCounterSupplier() {
    return ticksCounterSupplier;
  }

  /**
   * @return {@code Duration.ofNanos(200)} by default.
   *
   * @see RateMeter#getTimeSensitivity()
   */
  public final Duration getTimeSensitivity() {//todo return null by default and use 1/20 of samplesInterval
    return timeSensitivity;
  }

  /**
   * This configuration parameter specifies if
   * {@link AbstractRateMeter} must collect {@link RateMeterStats stats}.
   *
   * @return {@code true} by default.
   */
  public final boolean isCollectStats() {
    return collectStats;
  }

  /**
   * Specifies the maximum number of attempts to calculate the number of current ticks
   * (see {@link RateMeter#ticksCount()} for example).
   * Some implementations may allow a race condition while performing such a calculation for performance reasons
   * (such a race may be caused by allowing to move the samples window while counting current ticks).
   * Note that this is just a hint, so an implementation may choose to do more attempts,
   * but the number of attempts must be finite.
   *
   * @return 5 by default.
   */
  public final int getMaxTicksCountAttempts() {
    return maxTicksCountAttempts;
  }

  /**
   * Specifies the length of samples history measured in {@linkplain RateMeter#getSamplesInterval() samples interval units}.
   * Note that the specification of {@link RateMeter#rate(long)} implies that any {@link RateMeter}
   * must maintain samples history for at least 2 samples intervals.
   * Actual samples history length maintained by {@link RateMeter} must be within [HL; HL + 1].
   *
   * @return 3 by default.
   */
  public final int getHl() {
    return hl;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "(ticksCounterSupplier=" + ticksCounterSupplier
        + ", timeSensitivity=" + timeSensitivity
        + ", collectStats=" + collectStats
        + ", maxTicksCountAttempts=" + maxTicksCountAttempts
        + ", hl=" + hl
        + ')';
  }

  @NotThreadSafe
  public static class Builder {
    protected Function<Long, ? extends TicksCounter> ticksCounterSupplier;
    protected Duration timeSensitivity;
    protected boolean collectStats;
    protected int maxTicksCountAttempts;
    protected int hl;

    protected Builder() {
      ticksCounterSupplier = LongAdderTicksCounter::new;
      timeSensitivity = Duration.ofNanos(200);
      collectStats = true;
      maxTicksCountAttempts = 5;
      hl = 3;
    }

    protected Builder(final RateMeterConfig config) {
      ticksCounterSupplier = config.getTicksCounterSupplier();
      timeSensitivity = config.getTimeSensitivity();
      collectStats = config.isCollectStats();
      maxTicksCountAttempts = config.getMaxTicksCountAttempts();
      hl = config.getHl();
    }

    /**
     * @param ticksCounterSupplier Must be immutable.
     *
     * @see RateMeterConfig#getTicksCounterSupplier()
     */
    public final Builder setTicksCounterSupplier(final Function<Long, ? extends TicksCounter> ticksCounterSupplier) {
      checkNotNull(ticksCounterSupplier, "ticksCounterSupplier");
      this.ticksCounterSupplier = ticksCounterSupplier;
      return this;
    }

    /**
     * @param timeSensitivity Must be positive (not {@linkplain Duration#isNegative() negative} and not {@linkplain Duration#isZero() zero}).
     *
     * @see RateMeterConfig#getTimeSensitivity()
     */
    public final Builder setTimeSensitivity(final Duration timeSensitivity) {
      checkNotNull(timeSensitivity, "timeSensitivity");
      checkArgument(!timeSensitivity.isNegative(), "timeSensitivity", "Must be positive");
      checkArgument(!timeSensitivity.isZero(), "timeSensitivity", "Must not be zero");
      this.timeSensitivity = timeSensitivity;
      return this;
    }

    /**
     * @see RateMeterConfig#isCollectStats()
     */
    public final Builder setCollectStats(final boolean collectStats) {
      this.collectStats = collectStats;
      return this;
    }

    /**
     * @param maxTicksCountAttempts Must be positive.
     *
     * @see RateMeterConfig#getMaxTicksCountAttempts()
     */
    public final Builder setMaxTicksCountAttempts(final int maxTicksCountAttempts) {
      checkArgument(maxTicksCountAttempts > 0, "maxTicksCountAttempts", "Must be positive");
      this.maxTicksCountAttempts = maxTicksCountAttempts;
      return this;
    }

    /**
     * @param hl Must be greater than or equal to 2.
     *
     * @see RateMeterConfig#getHl()
     */
    public final Builder setHl(final int hl) {
      checkArgument(hl >= 2, "hl", "Must be greater than or equal to 2");
      this.hl = hl;
      return this;
    }

    public RateMeterConfig build() {
      return new RateMeterConfig(
          ticksCounterSupplier,
          timeSensitivity,
          collectStats,
          maxTicksCountAttempts,
          hl);
    }
  }
}