package stinc.male.sandbox.ratmex.executor;

import java.time.Duration;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import static stinc.male.sandbox.ratmex.internal.util.ConversionsAndChecks.checkDuration;
import static stinc.male.sandbox.ratmex.internal.util.Preconditions.checkArgument;
import static stinc.male.sandbox.ratmex.internal.util.Preconditions.checkNotNull;

/**
 * A configuration of a {@linkplain RateMeasuringExecutorService#scheduleAtFixedRate(Runnable, Rate, ScheduleConfig) scheduled task}.
 */
@Immutable
public class ScheduleConfig {
  private final Duration initialDelay;
  @Nullable
  private final Duration duration;
  @Nullable
  private final RateListener rateListener;

  /**
   * @param initialDelay See {@link Builder#setInitialDelay(Duration)}.
   * @param duration See {@link Builder#setDuration(Duration)}.
   * @param rateListener See {@link Builder#setRateListener(RateListener)}.
   */
  protected ScheduleConfig(
      final Duration initialDelay,
      @Nullable final Duration duration,
      @Nullable final RateListener rateListener) {
    checkDuration(initialDelay, "initialDelay");
    if (duration != null) {
      checkArgument(!duration.isZero(), "duration", "Must not be zero");
      checkArgument(!duration.isNegative(), "duration", "Must not be negative");
    }
    this.initialDelay = initialDelay;
    this.duration = duration;
    this.rateListener = rateListener;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public Builder toBuilder() {
    return new Builder(this);
  }

  /**
   * Specifies the time to delay the first execution of a scheduled task.
   *
   * @return {@link Duration#ZERO} by default.
   */
  public final Duration getInitialDelay() {
    return initialDelay;
  }

  /**
   * Specifies the amount of time after the {@linkplain #getInitialDelay() initial delay} after which a scheduled task should not be executed anymore.
   * There is no guarantee beyond best-effort attempt to not exceed this duration.
   * <p>
   * An {@linkplain Optional#empty() empty} duration means that the task will be repeatedly executed
   * until {@link RateMeasuringExecutorService} {@linkplain RateMeasuringExecutorService#isTerminated() is terminated}.
   *
   * @return An {@linkplain Optional#empty() empty} {@link Optional} by default.
   */
  public final Optional<Duration> getDuration() {
    return Optional.ofNullable(duration);
  }

  /**
   * TODO
   */
  public final Optional<RateListener> getRateListener() {
    return Optional.ofNullable(rateListener);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "{initialDelay=" + initialDelay
        + ", duration=" + duration
        + ", rateListener=" + rateListener
        + '}';
  }

  @NotThreadSafe
  public static class Builder {
    protected Duration initialDelay;
    @Nullable
    protected Duration duration;
    @Nullable
    protected RateListener rateListener;

    protected Builder() {
      initialDelay = Duration.ZERO;
      duration = null;
      rateListener = null;//TODO create default
    }

    /**
     * @param config Must not be null.
     */
    protected Builder(final ScheduleConfig config) {
      checkNotNull(config, "config");
      initialDelay = config.getInitialDelay();
      duration = config.getDuration()
          .orElse(null);
      rateListener = config.getRateListener()
          .orElse(null);
    }

    /**
     * @param initialDelay Must not be {@linkplain Duration#isNegative() negative}.
     *
     * @see ScheduleConfig#getInitialDelay()
     */
    public final ScheduleConfig.Builder setInitialDelay(final Duration initialDelay) {
      checkDuration(initialDelay, "initialDelay");
      this.initialDelay = initialDelay;
      return this;
    }

    /**
     * @see ScheduleConfig#getDuration()
     */
    public final ScheduleConfig.Builder setDuration(@Nullable final Duration duration) {
      if (duration != null) {
        checkArgument(!duration.isZero(), "duration", "Must not be zero");
        checkArgument(!duration.isNegative(), "duration", "Must not be negative");
      }
      this.duration = duration;
      return this;
    }

    /**
     * @see ScheduleConfig#getRateListener()
     */
    public final ScheduleConfig.Builder setRateListener(@Nullable final RateListener rateListener) {
      this.rateListener = rateListener;
      return this;
    }

    public ScheduleConfig build() {
      return new ScheduleConfig(
          initialDelay,
          duration,
          rateListener);
    }
  }
}