package stinc.male.sandbox.ratmex.executor;

import javax.annotation.concurrent.NotThreadSafe;
import stinc.male.sandbox.ratmex.meter.RateMeterReading;
import static stinc.male.sandbox.ratmex.internal.util.Preconditions.checkNotNull;

/**
 * TODO
 */
@NotThreadSafe
public class RateMeasuredEvent {
  private final Rate targetRate;
  private final RateMeterReading completionRate;

  public RateMeasuredEvent(final Rate targetRate, final RateMeterReading completionRate) {
    checkNotNull(targetRate, "targetRate");
    checkNotNull(completionRate, "completionRate");
    this.targetRate = targetRate;
    this.completionRate = completionRate;
  }

  public final Rate getTargetRate() {
    return targetRate;
  }

  public final RateMeterReading getCompletionRate() {
    return completionRate;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() +
        "{targetRate=" + targetRate +
        ", completionRate=" + completionRate +
        '}';
  }
}