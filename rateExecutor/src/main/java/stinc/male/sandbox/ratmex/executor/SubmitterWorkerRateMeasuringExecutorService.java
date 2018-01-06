package stinc.male.sandbox.ratmex.executor;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.concurrent.ThreadSafe;

/**
 * This {@link RateMeasuringExecutorService} assigns two roles to its threads: submitter and worker.
 * Threads belonging to different roles are organized in two {@link ExecutorService}'s of the same name.
 * A submitter submits tasks to worker with the target rate. A worker executes the submitted tasks as fast as it can.
 * If the worker manages to continuously execute all the tasks being submitted,
 * then the tasks are not only being submitted, but also are being completed with the target rate.
 * <p>
 * <b>Implementation notes</b><br>
 * A submitter always has exactly 1 thread, and a worker may have an arbitrary number of threads, including 0 (current-thread executor).
 * If a worker has no threads, then a submitter takes over worker role and executes tasks by itself.
 * <p>
 * Submitting tasks by a submitter to a worker means with necessity that submitter and worker threads communicate.
 * Inter-thread communication is always bad for performance, so it should be reduced to a minimum.
 * In order to do so a submitter organizes tasks in batches before submitting them to a worker.
 * Batching allows submitting multiple tasks to a worker via {@linkplain ExecutorService#execute(Runnable) a single act of communication}
 * between a submitter and a worker. However such a technique should be used with caution,
 * because it may induce an uneven load on worker threads and hence lead to a violation of the target rate of completion of tasks.
 * So in order to use batching, a submitter must know the number of worker threads and distribute batched tasks evenly among worker threads.
 */
@ThreadSafe
public final class SubmitterWorkerRateMeasuringExecutorService implements RateMeasuringExecutorService {
  /**
   * @param shutdownExternal A flag that specifies whether the externally provided submitter and the worker must be
   * shut down when this {@link ExecutorService} is shutting down.
   */
  public SubmitterWorkerRateMeasuringExecutorService(
      final ScheduledExecutorService submitter,
      final ExecutorService worker,
      final int workerThreads,
      final boolean shutdownExternal) {
  }

  @Override
  public final ScheduledFuture<?> scheduleAtFixedRate(final Runnable task, final Rate rate) throws RejectedExecutionException {
    return null;
  }

  @Override
  public final ScheduledFuture<?> scheduleAtFixedRate(final Runnable task, final Rate rate, final ScheduleConfig config) {
    return null;
  }

  @Override
  public final void shutdown() {
  }

  @Override
  public final List<Runnable> shutdownNow() {
    return null;
  }

  @Override
  public final boolean isShutdown() {
    return false;
  }

  @Override
  public final boolean isTerminated() {
    return false;
  }

  @Override
  public final boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
    return false;
  }

  @Override
  public final <T> Future<T> submit(final Callable<T> task) {
    return null;
  }

  @Override
  public final <T> Future<T> submit(final Runnable task, final T result) {
    return null;
  }

  @Override
  public final Future<?> submit(final Runnable task) {
    return null;
  }

  @Override
  public final <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks) throws InterruptedException {
    return null;
  }

  @Override
  public final <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit) throws
      InterruptedException {
    return null;
  }

  @Override
  public final <T> T invokeAny(final Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
    return null;
  }

  @Override
  public final <T> T invokeAny(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit) throws InterruptedException,
      ExecutionException,
      TimeoutException {
    return null;
  }

  @Override
  public final void execute(final Runnable command) {

  }
}