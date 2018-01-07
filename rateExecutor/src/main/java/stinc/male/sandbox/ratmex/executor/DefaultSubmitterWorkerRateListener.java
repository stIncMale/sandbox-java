package stinc.male.sandbox.ratmex.executor;

import javax.annotation.concurrent.Immutable;

/**
 * A default implementation of {@link RateListener}
 * which {@linkplain #onMeasurement(RateMeasuredEvent) throws} {@link RateFailedException} if notices that the target rate is not respected
 * by either {@linkplain SubmitterWorkerRateMeasuredEvent#getSubmissionRate() submitter}
 * or {@linkplain SubmitterWorkerRateMeasuredEvent#getCompletionRate() worker}.
 *
 * @param <E> A type of a {@link SubmitterWorkerRateMeasuredEvent} which this listener can react to.
 */
@Immutable
public class DefaultSubmitterWorkerRateListener<E extends SubmitterWorkerRateMeasuredEvent> extends DefaultRateListener<E> {
  private static final DefaultSubmitterWorkerRateListener<SubmitterWorkerRateMeasuredEvent> instance = new DefaultSubmitterWorkerRateListener<>();

  protected DefaultSubmitterWorkerRateListener() {
  }

  /**
   * @param <E> A type of a {@link SubmitterWorkerRateMeasuredEvent} which the returned listener can react to.
   *
   * @return A default {@link RateListener} for {@link SubmitterWorkerRateMeasuredEvent}.
   */
  @SuppressWarnings("unchecked")
  public static final <E extends SubmitterWorkerRateMeasuredEvent>
  DefaultSubmitterWorkerRateListener<E> defaultSubmitterWorkerRateListenerInstance() {
    return (DefaultSubmitterWorkerRateListener<E>)instance;
  }

  /**
   * @throws RateFailedException If the {@linkplain RateMeasuredEvent#getTargetRate() target rate} is not respected
   * by either {@linkplain SubmitterWorkerRateMeasuredEvent#getSubmissionRate() submitter}
   * or {@linkplain SubmitterWorkerRateMeasuredEvent#getCompletionRate() worker}.
   */
  @Override
  public boolean onMeasurement(final SubmitterWorkerRateMeasuredEvent event) throws RateFailedException {
    //TODO
    return false;
  }
}