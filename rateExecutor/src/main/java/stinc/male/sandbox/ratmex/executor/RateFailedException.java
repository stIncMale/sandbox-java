package stinc.male.sandbox.ratmex.executor;

/**
 * See {@link RateListener#onMeasurement(RateMeasuredEvent)}.
 */
public final class RateFailedException extends RuntimeException {
  static final long serialVersionUID = 8127363940038363975L;

  public RateFailedException() {
  }
}