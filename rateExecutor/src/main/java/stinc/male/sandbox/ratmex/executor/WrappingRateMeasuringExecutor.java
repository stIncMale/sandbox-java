package stinc.male.sandbox.ratmex.executor;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public final class WrappingRateMeasuringExecutor implements RateMeasuringExecutor {
  /**
   * @param shutdownExternal A flag that specifies whether the submitter and the worker must be
   * {@link ExecutorService#shutdownNow() shut down} by {@link #shutdownNow()}.
   */
  public WrappingRateMeasuringExecutor(final boolean shutdownExternal) {
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
  public final boolean awaitTermination(final Duration timeout) {
    return false;
  }

  @Override
  public final boolean isTerminated() {
    return false;
  }

  @Override
  public final RateMeasuringExecutor shutdownNow() {
    return null;
  }
}