package stinc.male.sandbox.ratmex.executor;

import javax.annotation.concurrent.ThreadSafe;
import stinc.male.sandbox.ratmex.meter.ConcurrentRateMeterStats;

/**
 * A default implementation of {@link RateListener}
 * which {@linkplain #onMeasurement(RateMeasuredEvent) throws} {@link RateFailedException} if notices that the target rate is not respected
 * by either {@linkplain SubmitterWorkerRateMeasuredEvent#getSubmissionRate() submitter}
 * or {@linkplain SubmitterWorkerRateMeasuredEvent#getCompletionRate() worker}.
 *
 * @param <E> A type of a {@link SubmitterWorkerRateMeasuredEvent} which this listener can react to.
 */
@ThreadSafe
public class DefaultSubmitterWorkerRateListener<E extends SubmitterWorkerRateMeasuredEvent<SRS, WRS>, SRS, WRS extends ConcurrentRateMeterStats>
    extends DefaultRateListener<E> {
  private static final DefaultSubmitterWorkerRateListener<?, ?, ?> instance = new DefaultSubmitterWorkerRateListener<>();

  protected DefaultSubmitterWorkerRateListener() {
  }

  /**
   * @param <E> A type of a {@link SubmitterWorkerRateMeasuredEvent} which the returned listener can react to.
   *
   * @return A default {@link RateListener} for {@link SubmitterWorkerRateMeasuredEvent}.
   */
  @SuppressWarnings("unchecked")
  public static final <E extends SubmitterWorkerRateMeasuredEvent<SRS, WRS>, SRS, WRS extends ConcurrentRateMeterStats>
  DefaultSubmitterWorkerRateListener<E, SRS, WRS> defaultSubmitterWorkerRateListenerInstance() {
    return (DefaultSubmitterWorkerRateListener<E, SRS, WRS>)instance;
  }

  /**
   * @throws RateFailedException If the {@linkplain RateMeasuredEvent#getTargetRate() target rate} is not respected
   * by either {@linkplain SubmitterWorkerRateMeasuredEvent#getSubmissionRate() submitter}
   * or {@linkplain SubmitterWorkerRateMeasuredEvent#getCompletionRate() worker}.
   */
  @Override
  public boolean onMeasurement(final E e) throws RateFailedException {
    super.onMeasurement(e);
    if (e.getTargetRate()
        .compareTo(e.getSubmissionRate()) != 0) {
      throw new RateFailedException("The submission rate violated the target rate. ", e.getTargetRate(), e.getSubmissionRate()
          .getValueDouble());
    }
    if (e.getWorkerRateMeterStats()
        .map(ConcurrentRateMeterStats::incorrectlyRegisteredTicksEventsCount)
        .orElse(0L) > 0) {
      throw new RuntimeException("Worker rate meter railed to accurately register ticks. " +
          "Consider increasing the samples history length, " +
          "or specifying the target rate by using a greater unit, " +
          "or switching to a strict mode. " +
          "actualRateValue=" + e.getCompletionRate());
    }
    return true;
  }
}