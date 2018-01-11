package stinc.male.sandbox.ratmex.executor;

import javax.annotation.concurrent.ThreadSafe;

/**
 * A default implementation of {@link RateListener}
 * which {@linkplain #onMeasurement(RateMeasuredEvent) throws} {@link RateFailedException} if notices that the target rate is not respected.
 *
 * @param <E> A type of a {@link RateMeasuredEvent} which this listener can react to.
 */
@ThreadSafe
public class DefaultRateListener<E extends RateMeasuredEvent> implements RateListener<E> {
  private static final DefaultRateListener<RateMeasuredEvent> instance = new DefaultRateListener<>();

  protected DefaultRateListener() {
  }

  /**
   * @param <E> A type of a {@link RateMeasuredEvent} which the returned listener can react to.
   *
   * @return A default {@link RateListener}.
   */
  @SuppressWarnings("unchecked")
  public static final <E extends RateMeasuredEvent> DefaultRateListener<E> defaultRateListenerInstance() {
    return (DefaultRateListener<E>)instance;
  }

  /**
   * @throws RateFailedException If the {@linkplain RateMeasuredEvent#getTargetRate() target rate} is not respected.
   */
  @Override
  public boolean onMeasurement(final E e) throws RateFailedException {
    if (e.getTargetRate()
        .compareTo(e.getCompletionRate()) != 0) {
      throw new RateFailedException(
          "The completion rate violated the target rate. ",
          e.getTargetRate(),
          e.getCompletionRate()
              .getValueDouble());
    }
    return true;
  }
}