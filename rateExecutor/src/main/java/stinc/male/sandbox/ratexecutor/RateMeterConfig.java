package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import java.util.function.Function;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import static stinc.male.sandbox.ratexecutor.Preconditions.checkArgument;
import static stinc.male.sandbox.ratexecutor.Preconditions.checkNotNull;

/**
 * The {@code @}{@link Immutable} here only guarantees that the {@code get...} methods
 * always behave as methods of immutable class.
 */
@Immutable
public class RateMeterConfig {
  private final boolean checkArguments;
  private final Function<Long, ? extends TicksCounter> ticksCounterSupplier;
  private final Duration timeSensitivity;
  private final boolean collectStats;

  protected RateMeterConfig(
      final boolean checkArguments,
      final Function<Long, ? extends TicksCounter> ticksCounterSupplier,
      final Duration timeSensitivity,
      final boolean collectStats) {
    this.checkArguments = checkArguments;
    this.ticksCounterSupplier = ticksCounterSupplier;
    this.timeSensitivity = timeSensitivity;
    this.collectStats = collectStats;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public Builder toBuilder() {
    return new Builder(this);
  }

  /**
   * Checking of arguments of {@link RateMeter} methods requires some computations,
   * but is not practical in most cases. This configuration parameter specifies if
   * {@link AbstractRateMeter} MUST do this checking, or MUST omit it.
   * @return {@code false} by default.
   */
  public final boolean isCheckArguments() {
    return checkArguments;
  }

  /**
   * Specifies a supplier which MUST be used by {@link AbstractRateMeter} to create ticks counters.
   * Note that if {@link AbstractRateMeter} is used concurrently
   * then supplier MUST provide a thread-safe implementation of {@link TicksCounter}.
   * @return {@code LongAdderTicksCounter::new} by default.
   */
  public final Function<Long, ? extends TicksCounter> getTicksCounterSupplier() {
    return ticksCounterSupplier;
  }

  /**
   * Specifies the time sensitivity which affects behaviour of {@link AbstractRateMeter#tick(long, long)} method
   * in a way that allows an implementation to score the specified sample at an instant
   * that differ from the specified one not more than by the time sensitivity.
   * <p>
   * It is recommended to specify here an resolution (accuracy, granularity) of the used timer.
   * @return {@code Duration.ofNanos(50)} by default. As of year 2017 there is no need to use
   * a finer sensitivity because it is an approximation of the {@link System#nanoTime()} resolution
   * (see <a href="https://github.com/shipilev/timers-bench">timers-bench</a>
   * and <a href="https://shipilev.net/blog/2014/nanotrusting-nanotime/">Nanotrusting the Nanotime</a> for measurements and explanations).
   */
  public final Duration getTimeSensitivity() {
    return timeSensitivity;
  }

  /**
   * This configuration parameter specifies if
   * {@link AbstractRateMeter} must collect stats.
   * @return {@code true} by default.
   */
  public final boolean isCollectStats() {
    return collectStats;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "(checkArguments=" + checkArguments
        + ", ticksCounterSupplier=" + ticksCounterSupplier
        + ", timeSensitivity=" + timeSensitivity
        + ", collectStats=" + collectStats
        + ')';
  }

  @NotThreadSafe
  public static class Builder {
    protected boolean checkArguments;
    protected Function<Long, ? extends TicksCounter> ticksCounterSupplier;
    protected Duration timeSensitivity;
    protected boolean collectStats;

    protected Builder() {
      checkArguments = false;
      ticksCounterSupplier = LongAdderTicksCounter::new;
      timeSensitivity = Duration.ofNanos(50);
      collectStats = true;
    }

    protected Builder(final RateMeterConfig config) {
      checkArguments = config.isCheckArguments();
      ticksCounterSupplier = config.getTicksCounterSupplier();
      timeSensitivity = config.getTimeSensitivity();
      collectStats = config.isCollectStats();
    }

    /**
     * @see RateMeterConfig#isCheckArguments()
     */
    public Builder setCheckArguments(final boolean checkArguments) {
      this.checkArguments = checkArguments;
      return this;
    }

    /**
     * @param ticksCounterSupplier Must be immutable.
     * @see RateMeterConfig#getTicksCounterSupplier()
     */
    public Builder setTicksCounterSupplier(final Function<Long, ? extends TicksCounter> ticksCounterSupplier) {
      checkNotNull(ticksCounterSupplier, "ticksCounterSupplier");
      this.ticksCounterSupplier = ticksCounterSupplier;
      return this;
    }

    /**
     * @param timeSensitivity Must be positive (not {@linkplain Duration#isNegative() negative} and not {@linkplain Duration#isZero() zero}).
     * @see RateMeterConfig#getTimeSensitivity()
     */
    public Builder setTimeSensitivity(final Duration timeSensitivity) {
      checkNotNull(timeSensitivity, "timeSensitivity");
      checkArgument(!timeSensitivity.isNegative(), "timeSensitivity", "Must be positive");
      checkArgument(!timeSensitivity.isZero(), "timeSensitivity", "Must not be zero");
      this.timeSensitivity = timeSensitivity;
      return this;
    }

    /**
     * @see RateMeterConfig#isCollectStats()
     */
    public Builder setCollectStats(final boolean collectStats) {
      this.collectStats = collectStats;
      return this;
    }

    public RateMeterConfig build() {
      return new RateMeterConfig(
          checkArguments,
          ticksCounterSupplier,
          timeSensitivity,
          collectStats);
    }
  }
}