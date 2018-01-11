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
 * <p>
 * The default values:
 * <ul>
 * <li>{@link #getInitialDelay()} - {@link Duration#ZERO}</li>
 * <li>{@link #getDuration()} - {@link Optional}{@code .}{@linkplain Optional#empty() empty()}</li>
 * <li>{@link #getRateListener()} - TODO specify default which should extend DefaultRateListener</li>
 * </ul>
 *
 * @param <E> A type of container with data provided to {@link RateListener} by {@link RateMeasuringExecutorService}.
 */
@Immutable
public class ScheduleConfig<E extends RateMeasuredEvent> {
  private final Duration initialDelay;
  @Nullable
  private final Duration duration;
  @Nullable
  private final RateListener<? super E> rateListener;

  /**
   * @param initialDelay See {@link Builder#setInitialDelay(Duration)}.
   * @param duration See {@link Builder#setDuration(Duration)}.
   * @param rateListener See {@link Builder#setRateListener(RateListener)}.
   */
  protected ScheduleConfig(
      final Duration initialDelay,
      @Nullable final Duration duration,
      @Nullable final RateListener<? super E> rateListener) {
    checkDuration(initialDelay, "initialDelay");
    if (duration != null) {
      checkArgument(!duration.isZero(), "duration", "Must not be zero");
      checkArgument(!duration.isNegative(), "duration", "Must not be negative");
    }
    this.initialDelay = initialDelay;
    this.duration = duration;
    this.rateListener = rateListener;
  }

  public static final <E extends RateMeasuredEvent> Builder<E> newScheduleConfigBuilder() {
    return new Builder<>();
  }

  public Builder<E> toBuilder() {
    return new Builder<E>().set(this);
  }

  /**
   * Specifies the time to delay the first execution of a scheduled task.
   */
  public final Duration getInitialDelay() {
    return initialDelay;
  }

  /**
   * Specifies the amount of time after the {@linkplain #getInitialDelay() initial delay} after which a scheduled task should not be executed anymore.
   * There is no guarantee beyond best-effort attempt to not exceed this duration.
   * <p>
   * An {@linkplain Optional#empty() empty} duration means that the task will be repeatedly executed
   * until one of the exceptional completions specified by
   * {@link RateMeasuringExecutorService#scheduleAtFixedRate(Runnable, Rate, ScheduleConfig)} occur.
   */
  public final Optional<Duration> getDuration() {
    return Optional.ofNullable(duration);
  }

  /**
   * A listener allowing monitoring the rate and reacting if there are deviations from the
   * {@linkplain RateMeasuredEvent#getTargetRate() target rate}.
   */
  public final Optional<RateListener<? super E>> getRateListener() {
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
  public static class Builder<E extends RateMeasuredEvent> {
    protected Duration initialDelay;
    @Nullable
    protected Duration duration;
    @Nullable
    protected RateListener<? super E> rateListener;

    protected Builder() {
      initialDelay = Duration.ZERO;
      duration = null;
      rateListener = DefaultRateListener.defaultRateListenerInstance();
    }

    /**
     * @param config Must not be null.
     */
    public final Builder<E> set(final ScheduleConfig<E> config) {
      checkNotNull(config, "config");
      initialDelay = config.getInitialDelay();
      duration = config.getDuration()
          .orElse(null);
      rateListener = config.getRateListener()
          .orElse(null);
      return this;
    }

    /**
     * @param initialDelay Must not be {@linkplain Duration#isNegative() negative}.
     *
     * @see ScheduleConfig#getInitialDelay()
     */
    public final Builder<E> setInitialDelay(final Duration initialDelay) {
      checkDuration(initialDelay, "initialDelay");
      this.initialDelay = initialDelay;
      return this;
    }

    /**
     * @see ScheduleConfig#getDuration()
     */
    public final Builder<E> setDuration(@Nullable final Duration duration) {
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
    public final Builder<E> setRateListener(@Nullable final RateListener<? super E> rateListener) {
      this.rateListener = rateListener;
      return this;
    }

    public ScheduleConfig<E> build() {
      return new ScheduleConfig<>(
          initialDelay,
          duration,
          rateListener);
    }
  }
}