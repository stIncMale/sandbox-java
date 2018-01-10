package stinc.male.sandbox.ratmex.executor;

import javax.annotation.Nullable;
import static stinc.male.sandbox.ratmex.internal.util.Preconditions.checkNotNull;
import static stinc.male.sandbox.ratmex.internal.util.Util.format;

/**
 * See {@link RateListener#onMeasurement(RateMeasuredEvent)}.
 */
public final class RateFailedException extends RuntimeException {
  static final long serialVersionUID = 8127363940038363975L;

  private final Rate targetRate;
  private final double actualRateValue;

  /**
   * @param clarification A text that clarifies this exception. The {@linkplain RuntimeException#getMessage() message} is constructed as
   * {@code (clarification == null ? "" : clarification) + "targetRate=%s, actualRateValue=%s"},
   * where {@code %s} are substituted with the arguments of this constructor.
   * @param targetRate See {@link #getTargetRate()}. Must not be {@code null}.
   * @param actualRateValue See {@link #getActualRateValue()}.
   */
  public RateFailedException(@Nullable final String clarification, final Rate targetRate, final double actualRateValue) {
    super((clarification == null ? "" : clarification) +
        format("targetRate=%s, actualRateValue=%s", checkNotNull(targetRate, "targetRate"), actualRateValue));
    this.targetRate = targetRate;
    this.actualRateValue = actualRateValue;
  }

  /**
   * @return The target rate of completion of the
   * {@linkplain RateMeasuringExecutorService#scheduleAtFixedRate(Runnable, Rate, ScheduleConfig) scheduled task}.
   */
  public final Rate getTargetRate() {
    return targetRate;
  }

  /**
   * @return The actual rate value measured in {@link Rate#getUnit() units} specified by the {@code targetRate}.
   */
  public final double getActualRateValue() {
    return actualRateValue;
  }
}