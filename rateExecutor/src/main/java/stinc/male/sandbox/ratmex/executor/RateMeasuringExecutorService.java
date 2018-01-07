package stinc.male.sandbox.ratmex.executor;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A <b>Rat</b>e-<b>Me</b>asuring e<b>X</b>ecutor service (hence <b>RatMeX</b>)
 * which not only executes tasks with a fixed {@link Rate rate},
 * but also measures the actual rate of submission and completion of the tasks
 * and allows one to monitor the rate and react if the executor has failed to conform to the target rate.
 * <p>
 * <b>The reasoning behind {@link RateMeasuringExecutorService}</b><br>
 * The functionality described by this interface can not be directly obtained from
 * {@link ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, TimeUnit)}, which says the following regarding the task being scheduled:
 * <i>"If any execution of this task takes longer than its period, then subsequent executions may start late, but will not concurrently execute"</i>.
 * This tells us that:
 * <ul>
 * <li>{@link ScheduledExecutorService} is allowed to execute tasks with a lower rate than the target (and there is no easy way to check).</li>
 * <li>{@link ScheduledExecutorService} executes a scheduled task serially, which means that you can not easily benefit from multithreading,
 * and the rate is heavily limited by the time the task takes to complete.</li>
 * </ul>
 * {@link RateMeasuringExecutorService} overcomes both of the above points.
 *
 * @param <C> A type of schedule config used in {@link #scheduleAtFixedRate(Runnable, Rate, C)}.
 */
@ThreadSafe
public interface RateMeasuringExecutorService<C extends ScheduleConfig> extends ExecutorService, AutoCloseable {
  /**
   * This method is similar to {@link #scheduleAtFixedRate(Runnable, Rate, C)},
   * but the configuration is chosen by the implementation.
   *
   * @throws RejectedExecutionException If failed to conform to the target rate.
   * The returned {@link ScheduledFuture} is {@link ScheduledFuture#isCancelled() cancelled} in this case.
   * <p>
   * This behavior is different from the behavior of {@link #scheduleAtFixedRate(Runnable, Rate, C)},
   * where a user decides what to do in case of a failure.
   */
  ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Rate rate);

  /**
   * Schedules a {@code task} to be executed with a fixed {@code rate}.
   * <p>
   * The repeated execution of the {@code task} continues indefinitely until
   * one of the following exceptional completions occur:
   * <ul>
   * <li>The task is {@linkplain Future#cancel(boolean) explicitly cancelled} via the returned future.</li>
   * <li>The executor {@linkplain #isTerminated() terminates}, also resulting in task {@linkplain Future#cancel(boolean) cancellation}.</li>
   * <li>An execution of the task throws a {@link RuntimeException}.
   * In this case calling {@link Future#get() get()} on the returned future will throw {@link ExecutionException}.</li>
   * <li>TODO the end of execution timeout from config. No guarantee that not later</li>
   * <li>TODO RateListener decides to stop the execution</li>
   * </ul>
   * Subsequent executions are suppressed.  Subsequent calls to
   * {@link Future#isDone isDone()} on the returned future will
   * return {@code true}.
   *
   * @param task A task to schedule for execution. Must not be {@code null}.
   * @param rate A target rate of submission and completion of the task. Must not be {@code null}.
   * @param config An additional configuration. Must not be {@code null}.
   *
   * @return A {@link ScheduledFuture} representing pending completion of the {@code task}.
   * The future's {@link Future#get() get()} method will never return normally,
   * and will throw an exception upon task cancellation or abnormal termination of a task execution.
   */
  ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Rate rate, C config);

  /**
   * This method is equivalent to calling {@link #shutdownNow()}.
   */
  @Override
  default void close() {
    shutdownNow();
  }
}