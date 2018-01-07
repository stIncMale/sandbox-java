package stinc.male.sandbox.ratmex.executor;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * A listener that allows monitoring the rate and reacting if there are deviations from the {@linkplain RateMeasuredEvent#getTargetRate() target rate}.
 *
 * @param <E> A type of a {@link RateMeasuredEvent} which this listener can react to.
 */
@NotThreadSafe
public interface RateListener<E extends RateMeasuredEvent> {
  /**
   * This method is called by {@link RateMeasuringExecutorService} each time it decides it makes sense to notify about the
   * {@linkplain RateMeasuredEvent#getCompletionRate() current rate}. This method must never be called concurrently.
   * <p>
   * Any {@link RuntimeException} thrown from this method causes the
   * {@linkplain RateMeasuringExecutorService#scheduleAtFixedRate(Runnable, Rate, ScheduleConfig) scheduled execution}
   * for which the {@code event} was generated to terminate abnormally
   * (i.e. cause method {@linkplain ScheduledFuture#get()} to throw {@link ExecutionException}
   * which provides the thrown exception via {@link ExecutionException#getCause()}).
   *
   * @param event An event with data provided by {@link RateMeasuringExecutorService}.
   *
   * @return true if the {@linkplain RateMeasuringExecutorService#scheduleAtFixedRate(Runnable, Rate, ScheduleConfig) scheduled execution}
   * for which the {@code event} was generated must continue; otherwise it will be {@linkplain ScheduledFuture#cancel(boolean) cancelled}.
   *
   * @throws RateFailedException May be thrown if the {@linkplain RateMeasuredEvent#getTargetRate() target rate} is not respected.
   * Note that implementation of this method may choose to ignore the deviation and return true
   * instead thus continuing the scheduled repetitive execution.
   */
  boolean onMeasurement(E event) throws RateFailedException;
}