package stinc.male.sandbox.ratmex.executor;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public final class SubmitterWorkerRateMeasuringExecutorService implements RateMeasuringExecutorService {
  /**
   * @param shutdownExternal A flag that specifies whether the submitter and the worker must be
   * shut down when this .
   */
  public SubmitterWorkerRateMeasuringExecutorService(final boolean shutdownExternal) {
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