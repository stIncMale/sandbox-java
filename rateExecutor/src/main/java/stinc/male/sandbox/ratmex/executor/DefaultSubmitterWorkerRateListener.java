package stinc.male.sandbox.ratmex.executor;

import javax.annotation.concurrent.ThreadSafe;

/**
 * A default implementation of {@link RateListener}
 * which {@linkplain #onMeasurement(RateMeasuredEvent) throws} {@link RateFailedException} if notices that the target rate is not respected
 * by either {@linkplain SubmitterWorkerRateMeasuredEvent#getSubmissionRate() submitter}
 * or {@linkplain SubmitterWorkerRateMeasuredEvent#getCompletionRate() worker}.
 *
 * @param <E> A type of a {@link SubmitterWorkerRateMeasuredEvent} which this listener can react to.
 */
@ThreadSafe
public class DefaultSubmitterWorkerRateListener<E extends SubmitterWorkerRateMeasuredEvent<SRS, WRS>, SRS, WRS> extends DefaultRateListener<E> {
  private static final DefaultSubmitterWorkerRateListener<?, ?, ?> instance =
      new DefaultSubmitterWorkerRateListener<>();

  protected DefaultSubmitterWorkerRateListener() {
  }

  /**
   * @param <E> A type of a {@link SubmitterWorkerRateMeasuredEvent} which the returned listener can react to.
   *
   * @return A default {@link RateListener} for {@link SubmitterWorkerRateMeasuredEvent}.
   */
  @SuppressWarnings("unchecked")
  public static final <E extends SubmitterWorkerRateMeasuredEvent<SRS, WRS>, SRS, WRS>
  DefaultSubmitterWorkerRateListener<E, SRS, WRS> defaultSubmitterWorkerRateListenerInstance() {
    return (DefaultSubmitterWorkerRateListener<E, SRS, WRS>)instance;
  }

  /**
   * @throws RateFailedException If the {@linkplain RateMeasuredEvent#getTargetRate() target rate} is not respected
   * by either {@linkplain SubmitterWorkerRateMeasuredEvent#getSubmissionRate() submitter}
   * or {@linkplain SubmitterWorkerRateMeasuredEvent#getCompletionRate() worker}.
   */
  @Override
  public boolean onMeasurement(final E event) throws RateFailedException {
    //TODO
    return false;
  }
}