package stinc.male.sandbox.ratmex.executor;

import java.time.Duration;
import java.util.function.Function;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import stinc.male.sandbox.ratmex.meter.RateMeter;
import static stinc.male.sandbox.ratmex.internal.util.Preconditions.checkNotNull;

/**
 * A configuration of a {@linkplain SubmitterWorkerRateMeasuringExecutorService#scheduleAtFixedRate(Runnable, Rate, SubmitterWorkerScheduleConfig) scheduled task}
 * for {@link SubmitterWorkerRateMeasuringExecutorService}.
 *
 * @param <SRS> A type that represents {@linkplain RateMeter#stats() statistics} of submitter {@link RateMeter}.
 * @param <WRS> A type that represents {@linkplain RateMeter#stats() statistics} of worker {@link RateMeter}.
 */
@Immutable
public class SubmitterWorkerScheduleConfig<SRS, WRS> extends ScheduleConfig {
  private final Function<Rate, ? extends RateMeter<? extends SRS>> submitterRateMeterSupplier;
  private final Function<Rate, ? extends RateMeter<? extends WRS>> workerRateMeterSupplier;

  protected SubmitterWorkerScheduleConfig(
      final Duration delay,
      @Nullable final Duration duration,
      @Nullable final RateListener listener,
      final Function<Rate, ? extends RateMeter<? extends SRS>> submitterRateMeterSupplier,
      final Function<Rate, ? extends RateMeter<? extends WRS>> workerRateMeterSupplier) {
    super(delay, duration, listener);
    checkNotNull(submitterRateMeterSupplier, "submitterRateMeterSupplier");
    checkNotNull(workerRateMeterSupplier, "workerRateMeterSupplier");
    this.submitterRateMeterSupplier = submitterRateMeterSupplier;
    this.workerRateMeterSupplier = workerRateMeterSupplier;
  }
}