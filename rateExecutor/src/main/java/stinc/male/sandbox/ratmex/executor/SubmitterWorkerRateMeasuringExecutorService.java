package stinc.male.sandbox.ratmex.executor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import stinc.male.sandbox.ratmex.meter.RateMeter;
import static stinc.male.sandbox.ratmex.internal.util.Preconditions.checkArgument;
import static stinc.male.sandbox.ratmex.internal.util.Preconditions.checkNotNull;

/**
 * This {@link RateMeasuringExecutorService} assigns two roles to its threads: submitter and worker.
 * Threads belonging to different roles are organized in two {@link ExecutorService}'s named submitter and worker.
 * A submitter submits tasks to a worker with the target rate. A worker executes the submitted tasks as fast as it can.
 * If the worker manages to continuously execute all the tasks being submitted without accumulating them,
 * then the tasks are not only being submitted but also are being completed with the target rate.
 * <p>
 * <b>Implementation notes</b><br>
 * A submitter always has exactly 1 thread, and a worker may have an arbitrary fixed number of threads
 * including 0 (current-thread executor) and varying number of threads.
 * If a worker has no threads, then a submitter takes over the worker role and executes tasks by itself.
 * <p>
 * Submitting tasks by a submitter to a worker means that the submitter and the worker threads communicate.
 * Inter-thread communication is always bad for performance, so it should be reduced to a minimum.
 * In order to do so a submitter organizes tasks in batches before submitting them to a worker.
 * Batching allows submitting multiple tasks to a worker via a single {@linkplain ExecutorService#execute(Runnable) act of communication}
 * between a submitter and a worker. However such a technique should be used with caution,
 * because it may induce an uneven load on worker threads and hence lead to a violation of the target rate of completion of tasks.
 * In order to use batching a submitter must know the number of worker threads (which must be fixed)
 * and distribute batched tasks evenly among worker threads.
 *
 * @param <C> A type of schedule config used in {@link #scheduleAtFixedRate(Runnable, Rate, C)}.
 * @param <E> TODO
 * @param <SRS> A type that represents {@linkplain RateMeter#stats() statistics} of submitter {@link RateMeter}.
 * @param <WRS> A type that represents {@linkplain RateMeter#stats() statistics} of worker {@link RateMeter}.
 */
@ThreadSafe
public class SubmitterWorkerRateMeasuringExecutorService
    <C extends SubmitterWorkerScheduleConfig<E, SRS, WRS>, E extends SubmitterWorkerRateMeasuredEvent<SRS, WRS>, SRS, WRS>
    implements RateMeasuringExecutorService<C, E> {
  private final ScheduledExecutorService submitter;
  @Nullable
  private final ExecutorService worker;
  private final int workerThreadsCount;
  private final AtomicBoolean startThreadsOnFirstTask;
  private final boolean shutdownSubmitterAndWorker;

  /**
   * This constructor wraps provided submitter and worker thus giving a user the maximum agility and control.
   *
   * @param submitter An externally provided submitter. Must have exactly 1 thread. If this constraint is violated, then the behavior is unspecified.
   * @param worker An externally provided worker. May have an arbitrary number of threads,
   * including 0 (current-thread executor) and varying number of threads.
   * The submitter submits tasks to the worker by using {@link ExecutorService#execute(Runnable)}.
   * @param workerThreadsCount The {@linkplain ThreadPoolExecutor#getMaximumPoolSize() maximum}
   * (and the {@linkplain ThreadPoolExecutor#getCorePoolSize() core}) number of threads in the worker executor.
   * {@code workerThreadsCount} \u2208 [-1,{@link Integer#MAX_VALUE}].
   * Use -1 if the number of worker threads is unknown or is varying, but in this case batching of tasks will be disabled.
   * If {@code workerThreadsCount} >= 0, then the number of worker threads must never be changed once they all were started.
   * If this constraint is violated, then the {@link RateMeasuringExecutorService} may fail to conform to the target rate because of batching.
   * {@linkplain #scheduleAtFixedRate(Runnable, Rate, C) scheduling} any task.
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
   * This constructor creates submitter and worker threads by using {@code submitterThreadFactory} and {@code workerThreadFactory} respectively.
   *
   * @param submitterThreadFactory A thread factory for submitter threads.
   * @param workerThreadFactory A thread factory for worker threads.
   * @param threadsCount A total number of both submitter and worker threads. {@code threadsCount} \u2208 [1,{@link Integer#MAX_VALUE}].
   * If {@code threadsCount} is 1, then the number of worker threads is 0, and the submitter takes over worker role and executes tasks by itself.
   * @param prestartThreads A flag that specifies if all submitter and worker threads must be started
   * upon the construction of {@link SubmitterWorkerRateMeasuringExecutorService}, causing them to idly wait for work.
   * If false, then all threads are started with the first {@linkplain #scheduleAtFixedRate(Runnable, Rate, C) scheduled task}.
   */
  public SubmitterWorkerRateMeasuringExecutorService(
      final ThreadFactory submitterThreadFactory,
      final ThreadFactory workerThreadFactory,
      final int threadsCount,
      final boolean prestartThreads) {
    this(
        createSubmitter(checkNotNull(submitterThreadFactory, "submitterThreadFactory"), prestartThreads),
        createWorker(checkNotNull(workerThreadFactory, "workerThreadFactory"), checkThreadsCountPositive(threadsCount) - 1, prestartThreads),
        threadsCount - 1,
        !prestartThreads,
        true);
  }

  /**
   * This constructor creates submitter and worker threads by using {@link Executors#defaultThreadFactory()}
   * and prepends either {@code "submitter-"} or {@code "worker-"} to each {@linkplain Thread#getName() thread name} respectively.
   *
   * @param threadsCount A total number of both submitter and worker threads. {@code threadsCount} \u2208 [1,{@link Integer#MAX_VALUE}].
   * If {@code threadsCount} is 1, then the number of worker threads is 0, and the submitter takes over worker role and executes tasks by itself.
   * @param prestartThreads A flag that specifies if all submitter and worker threads must be started
   * upon the construction of {@link SubmitterWorkerRateMeasuringExecutorService}, causing them to idly wait for work.
   * If false, then all threads are started with the first {@linkplain #scheduleAtFixedRate(Runnable, Rate, C) scheduled task}.
   */
  public SubmitterWorkerRateMeasuringExecutorService(final int threadsCount, final boolean prestartThreads) {
    this(
        createSubmitter(null, prestartThreads),
        createWorker(null, checkThreadsCountPositive(threadsCount) - 1, prestartThreads),
        threadsCount - 1,
        !prestartThreads,
        true);
  }

  private SubmitterWorkerRateMeasuringExecutorService(
      final ScheduledExecutorService submitter,
      @Nullable final ExecutorService worker,
      final int workerThreadsCount,
      final boolean startThreadsOnFirstTask,
      final boolean shutdownSubmitterAndWorker) {
    checkNotNull(submitter, "submitter");
    checkArgument(workerThreadsCount >= -1, "workerThreadsCount", "Must be greater than or equal to -1");
    assert (worker == null && workerThreadsCount == 0) || worker != null;
    this.submitter = submitter;
    this.worker = worker;
    this.workerThreadsCount = workerThreadsCount;
    this.startThreadsOnFirstTask = new AtomicBoolean(startThreadsOnFirstTask);
    this.shutdownSubmitterAndWorker = shutdownSubmitterAndWorker;
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(final Runnable task, final Rate targetRate) throws RejectedExecutionException {
    checkNotNull(task, "task");
    checkNotNull(targetRate, "targetRate");
    return scheduleAtFixedRate(task, targetRate, createScheduleConfig(targetRate));
  }

  /**
   * @param config An additional configuration. Must not be {@code null}.
   * <p>
   * Method {@link RateListener#onMeasurement(RateMeasuredEvent)} is called each time the submitter measures the current submission rate,
   * which in turn happens each time the submitter decides if new tasks need to be submitted to the worker.
   */
  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(final Runnable task, final Rate targetRate, final C config) {
    checkNotNull(task, "task");
    checkNotNull(targetRate, "targetRate");
    checkNotNull(config, "config");
    ensureAllThreadsStarted();
    return null;//TODO
  }

  /**
   * Delegates calls to the submitter and to the worker if and only if those were created by {@link SubmitterWorkerRateMeasuringExecutorService},
   * or were provided externally with enabled {@code shutdownSubmitterAndWorker} option.
   */
  @Override
  public void shutdown() {
    if (shutdownSubmitterAndWorker) {
      try {
        submitter.shutdown();
      } finally {
        if (worker != null) {
          worker.shutdown();
        }
      }
    }
  }

  /**
   * Delegates calls to the submitter and to the worker if and only if those were created by {@link SubmitterWorkerRateMeasuringExecutorService},
   * or were provided externally with enabled {@code shutdownSubmitterAndWorker} option.
   *
   * @return A list that contains combined results from the submitter and the worker,
   * or an {@linkplain Collections#emptyList() empty list} if the call was not delegated to them.
   */
  @Override
  public List<Runnable> shutdownNow() {
    final List<Runnable> result;
    if (shutdownSubmitterAndWorker) {
      result = new ArrayList<>();
      try {
        result.addAll(submitter.shutdownNow());
      } finally {
        if (worker != null) {
          result.addAll(worker.shutdownNow());
        }
      }
    } else {
      result = Collections.emptyList();
    }
    return result;
  }

  /**
   * Delegates calls to the submitter and to the worker.
   * Note that if the submitter and the worker were externally provided, then this method may return true
   * even if neither method {@link #shutdown()} nor {@link #shutdownNow()} was executed on this object.
   *
   * @return true if either the submitter or the worker return true; false otherwise.
   */
  @Override
  public boolean isShutdown() {
    return submitter.isShutdown() || actualWorker().isShutdown();
  }

  /**
   * Delegates calls to the submitter and to the worker.
   * Note that if the submitter and the worker were externally provided, then this method may return true
   * even if neither method {@link #shutdown()} nor {@link #shutdownNow()} was executed on this object.
   *
   * @return true if both the submitter and the worker return true; false otherwise.
   */
  @Override
  public boolean isTerminated() {
    return submitter.isTerminated() && actualWorker().isTerminated();
  }

  /**
   * Delegates calls to the submitter and to the worker, while making sure that both calls together do not take more than the specified timeout.
   * There is no guarantee beyond best-effort attempt to not exceed this duration.
   *
   * @return true if both the submitter and the worker return true; false otherwise.
   */
  @Override
  public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
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

  /**
   * Delegates calls to the worker.
   * <p>
   * {@inheritDoc}
   */
  @Override
  public <T> Future<T> submit(final Callable<T> task) {
    return actualWorker().submit(task);
  }

  /**
   * Delegates calls to the worker.
   * <p>
   * {@inheritDoc}
   */
  @Override
  public <T> Future<T> submit(final Runnable task, final T result) {
    return actualWorker().submit(task, result);
  }

  /**
   * Delegates calls to the worker.
   * <p>
   * {@inheritDoc}
   */
  @Override
  public Future<?> submit(final Runnable task) {
    return actualWorker().submit(task);
  }

  /**
   * Delegates calls to the worker.
   * <p>
   * {@inheritDoc}
   */
  @Override
  public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks) throws InterruptedException {
    return actualWorker().invokeAll(tasks);
  }

  /**
   * Delegates calls to the worker.
   * <p>
   * {@inheritDoc}
   */
  @Override
  public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit)
      throws InterruptedException {
    return actualWorker().invokeAll(tasks, timeout, unit);
  }

  /**
   * Delegates calls to the worker.
   * <p>
   * {@inheritDoc}
   */
  @Override
  public <T> T invokeAny(final Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
    return actualWorker().invokeAny(tasks);
  }

  /**
   * Delegates calls to the worker.
   * <p>
   * {@inheritDoc}
   */
  @Override
  public <T> T invokeAny(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return actualWorker().invokeAny(tasks, timeout, unit);
  }

  /**
   * Delegates calls to the worker.
   * <p>
   * {@inheritDoc}
   */
  @Override
  public void execute(final Runnable task) {
    actualWorker().execute(task);
  }

  private final ExecutorService actualWorker() {
    return worker == null ? submitter : worker;
  }

  private final void ensureAllThreadsStarted() {
    if (startThreadsOnFirstTask.get() && startThreadsOnFirstTask.compareAndSet(true, false)) {
      prestartAllThreads(submitter, 1);
      prestartAllThreads(worker, workerThreadsCount);
    }
  }

  private final C createScheduleConfig(final Rate rate) {
    return null;//TODO
  }

  private static final ScheduledExecutorService createSubmitter(@Nullable final ThreadFactory threadFactory, final boolean prestartThreads) {
    final ScheduledThreadPoolExecutor result = new ScheduledThreadPoolExecutor(1, ensureThreadFactory(threadFactory, "submitter-"));
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
          ensureThreadFactory(threadFactory, "worker-"));
      if (prestartThreads) {
        result.prestartAllCoreThreads();
      }
    }
    return result;
  }

  private static final ThreadFactory ensureThreadFactory(@Nullable final ThreadFactory threadFactory, @Nullable String namePrefix) {
    return threadFactory == null
        ? namePrefix == null ? Executors.defaultThreadFactory() : new NamePrefixThreadFactory(Executors.defaultThreadFactory(), namePrefix)
        : threadFactory;
  }

  /**
   * This method must only be called for {@code executor} that was constructed by {@link SubmitterWorkerRateMeasuringExecutorService}.
   */
  private static final void prestartAllThreads(final ExecutorService executor, final int threadsCount) {
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
}