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
 *
 * @param <E> TODO
 * @param <SRS> A type that represents {@linkplain RateMeter#stats() statistics} of submitter {@link RateMeter}.
 * @param <WRS> A type that represents {@linkplain RateMeter#stats() statistics} of worker {@link RateMeter}.
 */
@Immutable
public class SubmitterWorkerScheduleConfig<E extends RateMeasuredEvent, SRS, WRS> extends ScheduleConfig<E> {
  private final Function<Rate, ? extends RateMeter<? extends SRS>> submitterRateMeterSupplier;
  private final Function<Rate, ? extends RateMeter<? extends WRS>> workerRateMeterSupplier;

  protected SubmitterWorkerScheduleConfig(
      final Duration delay,
      @Nullable final Duration duration,
      @Nullable final RateListener<E> listener,
      final Function<Rate, ? extends RateMeter<? extends SRS>> submitterRateMeterSupplier,
      final Function<Rate, ? extends RateMeter<? extends WRS>> workerRateMeterSupplier) {
    super(delay, duration, listener);
    checkNotNull(submitterRateMeterSupplier, "submitterRateMeterSupplier");
    checkNotNull(workerRateMeterSupplier, "workerRateMeterSupplier");
    this.submitterRateMeterSupplier = submitterRateMeterSupplier;
    this.workerRateMeterSupplier = workerRateMeterSupplier;
  }

  public static final <E extends RateMeasuredEvent, SRS, WRS> Builder<E, SRS, WRS> newSubmitterWorkerScheduleConfigBuilder() {
    return new Builder<>();
  }

  public static final <E extends RateMeasuredEvent, SRS, WRS> Builder<E, SRS, WRS> newSubmitterWorkerScheduleConfigBuilder(
      final ScheduleConfig<E> config) {
    return new Builder<>(config);
  }

  @Override
  public Builder<E, SRS, WRS> toBuilder() {
    return new Builder<>(this);
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

    protected Builder(final SubmitterWorkerScheduleConfig<E, SRS, WRS> config) {
      this();
      checkNotNull(config, "config");
      submitterRateMeterSupplier = config.getSubmitterRateMeterSupplier();
      workerRateMeterSupplier = config.getWorkerRateMeterSupplier();
    }

    protected Builder(final ScheduleConfig<E> config) {
      super(config);
      //TODO
    }

    /**
     * @param config Must not be null.
     */
    public final Builder<E, SRS, WRS> set(final ScheduleConfig<E> config) {
      checkNotNull(config, "config");
      //TODO
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

    public Builder<E, SRS, WRS> buildSubmitterWorkerBuilder() {
      return null;//TODO
    }
  }
}