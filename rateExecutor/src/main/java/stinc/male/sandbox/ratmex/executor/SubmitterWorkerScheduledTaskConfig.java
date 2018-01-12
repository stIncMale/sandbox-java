package stinc.male.sandbox.ratmex.executor;

import java.time.Duration;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import stinc.male.sandbox.ratmex.meter.RateMeter;
import static stinc.male.sandbox.ratmex.internal.util.Preconditions.checkNotNull;

/**
 * A configuration of a {@linkplain SubmitterWorkerRateMeasuringExecutorService#scheduleAtFixedRate(Runnable, Rate, SubmitterWorkerScheduledTaskConfig) scheduled task}
 * for {@link SubmitterWorkerRateMeasuringExecutorService}.
 * <p>
 * The default values:
 * <ul>
 * <li>The default values from {@link ScheduledTaskConfig}</li>
 * <li>{@link #getSubmitterRateMeterSupplier()} - Must be explicitly specified, there is no default value</li>
 * <li>{@link #getWorkerRateMeterSupplier()} - Must be explicitly specified, there is no default value</li>
 * </ul>
 *
 * @param <E> A type of container with data provided to {@link RateListener} by {@link RateMeasuringExecutorService}.
 * @param <SRS> A type that represents {@linkplain RateMeter#stats() statistics} of submitter {@link RateMeter}.
 * @param <WRS> A type that represents {@linkplain RateMeter#stats() statistics} of worker {@link RateMeter}.
 */
/* submitterRateMeterSupplier and workerRateMeterSupplier do not have default values because this is technically impossible;
    problems arise because of the parameter types SRS and WRS. The similar problem is with ScheduledTaskConfig.rateListener*/
@Immutable
public class SubmitterWorkerScheduledTaskConfig<E extends RateMeasuredEvent, SRS, WRS> extends ScheduledTaskConfig<E> {
  private final BiFunction<Long, Rate, ? extends RateMeter<? extends SRS>> submitterRateMeterSupplier;
  private final BiFunction<Long, Rate, ? extends RateMeter<? extends WRS>> workerRateMeterSupplier;

  protected SubmitterWorkerScheduledTaskConfig(
      final Duration initialDelay,
      @Nullable final Duration duration,
      @Nullable final RateListener<? super E> rateListener,
      final BiFunction<Long, Rate, ? extends RateMeter<? extends SRS>> submitterRateMeterSupplier,
      final BiFunction<Long, Rate, ? extends RateMeter<? extends WRS>> workerRateMeterSupplier) {
    super(initialDelay, duration, rateListener);
    checkNotNull(submitterRateMeterSupplier, "submitterRateMeterSupplier");
    checkNotNull(workerRateMeterSupplier, "workerRateMeterSupplier");
    this.submitterRateMeterSupplier = submitterRateMeterSupplier;
    this.workerRateMeterSupplier = workerRateMeterSupplier;
  }

  public static final <E extends RateMeasuredEvent, SRS, WRS> Builder<E, SRS, WRS> newSubmitterWorkerScheduleConfigBuilder() {
    return new Builder<>();
  }

  @Override
  public Builder<E, SRS, WRS> toBuilder() {
    return new Builder<E, SRS, WRS>().set(this);
  }

  /**
   * Specifies a supplier which must be used by {@link SubmitterWorkerRateMeasuringExecutorService}
   * to create a {@link RateMeter} for measuring the submission rate.
   */
  public final BiFunction<Long, Rate, ? extends RateMeter<? extends SRS>> getSubmitterRateMeterSupplier() {
    return submitterRateMeterSupplier;
  }

  /**
   * Specifies a supplier which must be used by {@link SubmitterWorkerRateMeasuringExecutorService}
   * to create a {@link RateMeter} for measuring the completion rate.
   */
  public final BiFunction<Long, Rate, ? extends RateMeter<? extends WRS>> getWorkerRateMeterSupplier() {
    return workerRateMeterSupplier;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() +
        "{initialDelay=" + getInitialDelay() +
        ", duration=" + getDuration() +
        ", rateListener=" + getRateListener() +
        ", submitterRateMeterSupplier" + submitterRateMeterSupplier +
        ", workerRateMeterSupplier" + workerRateMeterSupplier +
        '}';
  }

  @NotThreadSafe
  public static class Builder<E extends RateMeasuredEvent, SRS, WRS> extends ScheduledTaskConfig.Builder<E> {
    private BiFunction<Long, Rate, ? extends RateMeter<? extends SRS>> submitterRateMeterSupplier;
    private BiFunction<Long, Rate, ? extends RateMeter<? extends WRS>> workerRateMeterSupplier;

    protected Builder() {
    }

    /**
     * @param config Must not be null.
     */
    public final Builder<E, SRS, WRS> set(final SubmitterWorkerScheduledTaskConfig<E, SRS, WRS> config) {
      checkNotNull(config, "config");
      set((ScheduledTaskConfig<E>)config);
      submitterRateMeterSupplier = config.getSubmitterRateMeterSupplier();
      workerRateMeterSupplier = config.getWorkerRateMeterSupplier();
      return this;
    }

    /**
     * @param submitterRateMeterSupplier Must not be null.
     *
     * @see #getSubmitterRateMeterSupplier()
     */
    public final Builder<E, SRS, WRS> setSubmitterRateMeterSupplier(final BiFunction<Long, Rate, ? extends RateMeter<? extends SRS>> submitterRateMeterSupplier) {
      checkNotNull(submitterRateMeterSupplier, "submitterRateMeterSupplier");
      this.submitterRateMeterSupplier = submitterRateMeterSupplier;
      return this;
    }

    /**
     * @param workerRateMeterSupplier Must not be null.
     *
     * @see #getWorkerRateMeterSupplier()
     */
    public final Builder<E, SRS, WRS> setWorkerRateMeterSupplier(final BiFunction<Long, Rate, ? extends RateMeter<? extends WRS>> workerRateMeterSupplier) {
      checkNotNull(workerRateMeterSupplier, "workerRateMeterSupplier");
      this.workerRateMeterSupplier = workerRateMeterSupplier;
      return this;
    }

    public final SubmitterWorkerScheduledTaskConfig<E, SRS, WRS> buildSubmitterWorkerScheduledTaskConfig() {
      return new SubmitterWorkerScheduledTaskConfig<>(
          initialDelay,
          duration,
          rateListener,
          submitterRateMeterSupplier,
          workerRateMeterSupplier);
    }
  }
}