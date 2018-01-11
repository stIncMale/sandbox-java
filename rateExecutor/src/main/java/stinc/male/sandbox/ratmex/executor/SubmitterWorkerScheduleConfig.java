package stinc.male.sandbox.ratmex.executor;

import java.time.Duration;
import java.util.function.Function;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import stinc.male.sandbox.ratmex.meter.RateMeter;
import static stinc.male.sandbox.ratmex.internal.util.Preconditions.checkNotNull;

/**
 * A configuration of a {@linkplain SubmitterWorkerRateMeasuringExecutorService#scheduleAtFixedRate(Runnable, Rate, SubmitterWorkerScheduleConfig) scheduled task}
 * for {@link SubmitterWorkerRateMeasuringExecutorService}.
 * <p>
 * The default values:
 * <ul>
 * <li>The default values from {@link ScheduleConfig}</li>
 * <li>{@link #getSubmitterRateMeterSupplier()} - TODO</li>
 * <li>{@link #getWorkerRateMeterSupplier()} - TODO</li>
 * </ul>
 *
 * @param <E> A type of container with data provided to {@link RateListener} by {@link RateMeasuringExecutorService}.
 * @param <SRS> A type that represents {@linkplain RateMeter#stats() statistics} of submitter {@link RateMeter}.
 * @param <WRS> A type that represents {@linkplain RateMeter#stats() statistics} of worker {@link RateMeter}.
 */
@Immutable
public class SubmitterWorkerScheduleConfig<E extends RateMeasuredEvent, SRS, WRS> extends ScheduleConfig<E> {
  private final Function<Rate, ? extends RateMeter<? extends SRS>> submitterRateMeterSupplier;
  private final Function<Rate, ? extends RateMeter<? extends WRS>> workerRateMeterSupplier;

  protected SubmitterWorkerScheduleConfig(
      final Duration initialDelay,
      @Nullable final Duration duration,
      @Nullable final RateListener<? super E> rateListener,
      final Function<Rate, ? extends RateMeter<? extends SRS>> submitterRateMeterSupplier,
      final Function<Rate, ? extends RateMeter<? extends WRS>> workerRateMeterSupplier) {
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
   * TODO
   */
  public final Function<Rate, ? extends RateMeter<? extends SRS>> getSubmitterRateMeterSupplier() {
    return submitterRateMeterSupplier;
  }

  /**
   * TODO
   */
  public final Function<Rate, ? extends RateMeter<? extends WRS>> getWorkerRateMeterSupplier() {
    return workerRateMeterSupplier;
  }

  @NotThreadSafe
  public static class Builder<E extends RateMeasuredEvent, SRS, WRS> extends ScheduleConfig.Builder<E> {
    private Function<Rate, ? extends RateMeter<? extends SRS>> submitterRateMeterSupplier;
    private Function<Rate, ? extends RateMeter<? extends WRS>> workerRateMeterSupplier;

    protected Builder() {
      //TODO
    }

    /**
     * @param config Must not be null.
     */
    public final Builder<E, SRS, WRS> set(final SubmitterWorkerScheduleConfig<E, SRS, WRS> config) {
      checkNotNull(config, "config");
      set((ScheduleConfig<E>)config);
      submitterRateMeterSupplier = config.getSubmitterRateMeterSupplier();
      workerRateMeterSupplier = config.getWorkerRateMeterSupplier();
      return this;
    }

    /**
     * @param submitterRateMeterSupplier Must not be null.
     *
     * @see #getSubmitterRateMeterSupplier()
     */
    public final Builder<E, SRS, WRS> setSubmitterRateMeterSupplier(final Function<Rate, ? extends RateMeter<? extends SRS>> submitterRateMeterSupplier) {
      checkNotNull(submitterRateMeterSupplier, "submitterRateMeterSupplier");
      this.submitterRateMeterSupplier = submitterRateMeterSupplier;
      return this;
    }

    /**
     * @param workerRateMeterSupplier Must not be null.
     *
     * @see #getWorkerRateMeterSupplier()
     */
    public final Builder<E, SRS, WRS> setWorkerRateMeterSupplier(final Function<Rate, ? extends RateMeter<? extends WRS>> workerRateMeterSupplier) {
      checkNotNull(workerRateMeterSupplier, "workerRateMeterSupplier");
      this.workerRateMeterSupplier = workerRateMeterSupplier;
      return this;
    }

    public SubmitterWorkerScheduleConfig<E, SRS, WRS> buildSubmitterWorkerBuilder() {
      return new SubmitterWorkerScheduleConfig<>(
          initialDelay,
          duration,
          rateListener,
          submitterRateMeterSupplier,
          workerRateMeterSupplier);
    }
  }
}