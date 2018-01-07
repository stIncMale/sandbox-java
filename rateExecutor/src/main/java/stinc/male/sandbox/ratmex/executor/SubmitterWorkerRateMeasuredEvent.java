package stinc.male.sandbox.ratmex.executor;

import stinc.male.sandbox.ratmex.meter.RateMeterReading;

/**
 * TODO
 */
public class SubmitterWorkerRateMeasuredEvent extends RateMeasuredEvent {
  private final RateMeterReading submissionRate;

  public SubmitterWorkerRateMeasuredEvent(
      final Rate targetRate,
      final RateMeterReading completionRate,
      final RateMeterReading submissionRate) {
    super(targetRate, completionRate);
    this.submissionRate = submissionRate;
  }

  public final RateMeterReading getSubmissionRate() {
    return submissionRate;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() +
        "{targetRate=" + getTargetRate() +
        ", completionRate=" + getCompletionRate() +
        ", submissionRate=" + submissionRate +
        '}';
  }
}