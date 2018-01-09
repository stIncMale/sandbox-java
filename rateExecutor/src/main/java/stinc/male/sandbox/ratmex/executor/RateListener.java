package stinc.male.sandbox.ratmex.executor;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;

/**
 * A listener allowing monitoring the rate and reacting if there are deviations from the {@linkplain RateMeasuredEvent#getTargetRate() target rate}.
 *
 * @param <E> A type of a {@link RateMeasuredEvent} which this listener can react to.
 */
public interface RateListener<E extends RateMeasuredEvent> {
  /**
   * This method is called by {@link RateMeasuringExecutorService} each time it decides it makes sense to notify about the
   * {@linkplain RateMeasuredEvent#getCompletionRate() current rate}.
   * This method must never be called concurrently, and implementations of {@link RateMeasuringExecutorService} must not call the method concurrently.
   * <p>
   * Any {@link RuntimeException} thrown from this method causes the
   * {@linkplain RateMeasuringExecutorService#scheduleAtFixedRate(Runnable, Rate, ScheduleConfig) scheduled execution}
   * for which the {@code event} was generated to terminate abnormally
   * (i.e. cause method {@linkplain ScheduledFuture#get()} to throw {@link ExecutionException}
   * which provides the thrown exception via {@link ExecutionException#getCause()}).
   *
   * @param event An event with data provided by {@link RateMeasuringExecutorService}.
   * An implementation of this method must not pass on a reference to the event
   * in a way that allows accessing the event outside of this method invocation because an event is mutable and its contents are only guaranteed to
   * stay unchanged within an invocation of this method. Data extracted from the event can, however, be transferred anywhere if that data is either
   * primitive or immutable.
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