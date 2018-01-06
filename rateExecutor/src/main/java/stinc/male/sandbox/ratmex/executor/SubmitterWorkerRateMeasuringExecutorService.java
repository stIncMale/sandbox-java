package stinc.male.sandbox.ratmex.executor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import static stinc.male.sandbox.ratmex.internal.util.Preconditions.checkArgument;
import static stinc.male.sandbox.ratmex.internal.util.Preconditions.checkNotNull;

/**
 * This {@link RateMeasuringExecutorService} assigns two roles to its threads: submitter and worker.
 * Threads belonging to different roles are organized in two {@link ExecutorService}'s of the same name.
 * A submitter submits tasks to worker with the target rate. A worker executes the submitted tasks as fast as it can.
 * If the worker manages to continuously execute all the tasks being submitted,
 * then the tasks are not only being submitted, but also are being completed with the target rate.
 * <p>
 * <b>Implementation notes</b><br>
 * A submitter always has exactly 1 thread, and a worker may have an arbitrary number of threads,
 * including 0 (current-thread executor) and varying number of threads.
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
  private final ScheduledExecutorService submitter;
  @Nullable
  private final ExecutorService worker;
  private final int workerThreadsCount;
  private final boolean prestartThreads;
  private final boolean shutdownSubmitterAndWorker;

  /**
   * @param submitter An externally provided submitter. Must have exactly 1 thread. If this constraint is violated, then the behavior is unspecified.
   * @param worker An externally provided worker. May have an arbitrary number of threads,
   * including 0 (current-thread executor) and varying number of threads.
   * @param workerThreadsCount The maximum (and core) number of threads in the worker executor.
   * {@code workerThreadsCount} \u2208 [-1,{@link Integer#MAX_VALUE}].
   * Use -1 if the number of worker threads is unknown or is varying, but in this case batching of tasks will be disabled.
   * If {@code workerThreadsCount} > 0, then the number of worker threads must never be changed once they all were started.
   * If this constraint is violated, then the {@link RateMeasuringExecutorService} may fail to conform to the target rate because of batching.
   * {@link RateMeasuringExecutorService} will force the worker to start all the threads before
   * {@linkplain #scheduleAtFixedRate(Runnable, Rate, ScheduleConfig) scheduling} any task.
   * @param shutdownSubmitterAndWorker A flag that specifies whether the externally provided submitter and the worker must be
   * shut down when this {@link ExecutorService} is shutting down.
   */
  public SubmitterWorkerRateMeasuringExecutorService(
      final ScheduledExecutorService submitter,
      final ExecutorService worker,
      final int workerThreadsCount,
      final boolean shutdownSubmitterAndWorker) {
    this(
        checkNotNull(submitter, "submitter"),
        checkNotNull(worker, "worker"),
        workerThreadsCount,
        false,
        shutdownSubmitterAndWorker);
  }

  /**
   * @param submitterThreadFactory
   * @param workerThreadFactory
   * @param workerThreadsCount
   * @param threadsCount
   * @param prestartThreads A flag that specifies if all submitter and worker threads must be started
   * upon the construction of {@link SubmitterWorkerRateMeasuringExecutorService}, causing them to idly wait for work.
   * If false, then all threads are started with the first task given to this executor.
   */
  public SubmitterWorkerRateMeasuringExecutorService(
      final ThreadFactory submitterThreadFactory,
      final ThreadFactory workerThreadFactory,
      final int workerThreadsCount,
      final int threadsCount,
      final boolean prestartThreads) {
    this(
        createSubmitter(checkNotNull(submitterThreadFactory, "submitterThreadFactory"), prestartThreads),
        createWorker(checkNotNull(workerThreadFactory, "workerThreadFactory"), checkThreadsCountNotNegative(workerThreadsCount), prestartThreads),
        workerThreadsCount,
        prestartThreads,
        true);
  }

  public SubmitterWorkerRateMeasuringExecutorService(final int threadsCount, final boolean prestartThreads) {
    this(
        createSubmitter(null, prestartThreads),
        createWorker(null, checkThreadsCountPositive(threadsCount) - 1, prestartThreads),
        threadsCount - 1,
        prestartThreads,
        true);
  }

  private SubmitterWorkerRateMeasuringExecutorService(
      final ScheduledExecutorService submitter,
      @Nullable final ExecutorService worker,
      final int workerThreadsCount,
      final boolean preStartThreads,
      final boolean shutdownSubmitterAndWorker) {
    checkNotNull(submitter, "submitter");
    checkArgument(workerThreadsCount >= -1, "workerThreadsCount", "Must be greater than or equal to -1");
    assert (worker == null && workerThreadsCount == 0) || worker != null;
    this.submitter = submitter;
    this.worker = worker;
    this.workerThreadsCount = workerThreadsCount;
    this.prestartThreads = preStartThreads;
    this.shutdownSubmitterAndWorker = shutdownSubmitterAndWorker;
  }

  @Override
  public final ScheduledFuture<?> scheduleAtFixedRate(final Runnable task, final Rate rate) throws RejectedExecutionException {
    return null;//TODO implement
  }

  @Override
  public final ScheduledFuture<?> scheduleAtFixedRate(final Runnable task, final Rate rate, final ScheduleConfig config) {
    return null;//TODO implement
  }

  @Override
  public final void shutdown() {
    try {
      submitter.shutdown();
    } finally {
      if (worker != null) {
        worker.shutdown();
      }
    }
  }

  @Override
  public final List<Runnable> shutdownNow() {
    final List<Runnable> result = new ArrayList<>();
    try {
      result.addAll(submitter.shutdownNow());
    } finally {
      if (worker != null) {
        result.addAll(worker.shutdownNow());
      }
    }
    return result;
  }

  @Override
  public final boolean isShutdown() {
    return submitter.isShutdown() || actualWorker().isShutdown();
  }

  @Override
  public final boolean isTerminated() {
    return submitter.isTerminated() && actualWorker().isTerminated();
  }

  @Override
  public final boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
    checkNotNull(unit, "unit");
    final long timeoutDurationNanos = unit.toNanos(timeout);
    final long startNanos = System.nanoTime();
    boolean result = false;
    try {
      result = submitter.awaitTermination(timeout, unit);
    } finally {
      if (worker != null) {
        final long passedDurationNanos = System.nanoTime() - startNanos;
        if (passedDurationNanos < timeoutDurationNanos) {
          result = result && worker.awaitTermination(timeoutDurationNanos - passedDurationNanos, TimeUnit.NANOSECONDS);
        }
      }
    }
    return result;
  }

  @Override
  public final <T> Future<T> submit(final Callable<T> task) {
    return actualWorker().submit(task);
  }

  @Override
  public final <T> Future<T> submit(final Runnable task, final T result) {
    return actualWorker().submit(task, result);
  }

  @Override
  public final Future<?> submit(final Runnable task) {
    return actualWorker().submit(task);
  }

  @Override
  public final <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks) throws InterruptedException {
    return actualWorker().invokeAll(tasks);
  }

  @Override
  public final <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit) throws
      InterruptedException {
    return actualWorker().invokeAll(tasks, timeout, unit);
  }

  @Override
  public final <T> T invokeAny(final Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
    return actualWorker().invokeAny(tasks);
  }

  @Override
  public final <T> T invokeAny(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit) throws InterruptedException,
      ExecutionException,
      TimeoutException {
    return actualWorker().invokeAny(tasks, timeout, unit);
  }

  @Override
  public final void execute(final Runnable task) {
    actualWorker().execute(task);
  }

  private final ExecutorService actualWorker() {
    return worker == null ? submitter : worker;
  }

  private static final ScheduledExecutorService createSubmitter(@Nullable final ThreadFactory threadFactory, final boolean prestartThreads) {
    final ScheduledThreadPoolExecutor result = new ScheduledThreadPoolExecutor(1, ensureThreadFactory(threadFactory));
    result.setMaximumPoolSize(1);
    result.setRemoveOnCancelPolicy(true);
    result.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    result.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
    if (prestartThreads) {
      result.prestartAllCoreThreads();
    }
    return result;
  }

  @Nullable
  private static final ExecutorService createWorker(
      @Nullable final ThreadFactory threadFactory,
      final int threadsCount,
      final boolean prestartThreads) {
    @Nullable
    final ThreadPoolExecutor result;
    if (threadsCount == 0) {
      result = null;
    } else {
      result = new ThreadPoolExecutor(
          threadsCount,
          threadsCount,
          0L,
          TimeUnit.MILLISECONDS,
          new LinkedBlockingQueue<>(),
          ensureThreadFactory(threadFactory));
      if (prestartThreads) {
        result.prestartAllCoreThreads();
      }
    }
    return result;
  }

  private static final ThreadFactory ensureThreadFactory(@Nullable final ThreadFactory threadFactory) {
    return threadFactory == null ? Executors.defaultThreadFactory() : threadFactory;
  }

  /**
   * This method must only be called for {@code executor} that was constructed by {@link SubmitterWorkerRateMeasuringExecutorService}.
   */
  private static final void prestartThreads(final ExecutorService executor, final int threadsCount) {
    assert executor instanceof ThreadPoolExecutor;
    final ThreadPoolExecutor ex = (ThreadPoolExecutor)executor;
    assert ex.getMaximumPoolSize() == ex.getCorePoolSize();
    assert ex.getCorePoolSize() == threadsCount;
    ex.prestartAllCoreThreads();
  }

  private static final int checkThreadsCountPositive(final int threadsCount) {
    checkArgument(threadsCount > 0, "threadsCount", "Must be positive");
    return threadsCount;
  }

  private static final int checkThreadsCountNotNegative(final int threadsCount) {
    checkArgument(threadsCount >= 0, "threadsCount", "Must not be negative");
    return threadsCount;
  }
}