package stinc.male.sandbox.ratexecutor.tmp;

import java.util.concurrent.Executor;
import java.util.concurrent.locks.LockSupport;
import javax.annotation.concurrent.ThreadSafe;
import stinc.male.sandbox.ratexecutor.RateMeter;

@ThreadSafe
public final class RatMeX {
  private final Executor executor;
  private final RateMeter rateMeter;

  public RatMeX(final Executor executor, final RateMeter rateMeter) {
    this.executor = executor;
    this.rateMeter = rateMeter;
  }

  public final void submit(final Runnable task, final ClosedInterval targetRateInterval) {
    executor.execute(new Task(task, targetRateInterval, executor, rateMeter));
  }

  private static final class Task implements Runnable {
    private final Runnable task;
    private final ClosedInterval targetRateInterval;
    private final Executor executor;
    private final RateMeter rateMeter;

    private Task(final Runnable task, final ClosedInterval targetRateInterval, final Executor executor, final RateMeter rateMeter) {
      this.task = task;
      this.targetRateInterval = targetRateInterval;
      this.executor = executor;
      this.rateMeter = rateMeter;
    }

    @Override
    public final void run() {
      try {
        task.run();
        final long tNanos = System.nanoTime();
        rateMeter.tick(1, tNanos);
        final double currentRate = rateMeter.rate(tNanos);
        if (currentRate < targetRateInterval.getMean()) {
          for (int i = 0; i < targetRateInterval.getMean() - currentRate; i++) {
            executor.execute(this);
          }
        } else {
          final double overRate = currentRate - targetRateInterval.getMean();
          sleep(tNanos, overRate / (targetRateInterval.getMean() / rateMeter.getSamplesInterval().toNanos()));
          executor.execute(this);
        }
      } catch (final Throwable cause) {
        System.out.println(cause);
      }
    }

    private static final void sleep(long tNanos, final double durationNanos) {//if one thread waits - all thread wait
      final double targetTNanos = tNanos + durationNanos;
      boolean interrupted = false;
      try {
        while (tNanos < targetTNanos) {
          LockSupport.parkNanos((long)durationNanos);
          if (Thread.interrupted()) {
            interrupted = true;
          }
          tNanos = System.nanoTime();
        }
      } finally {
        if (interrupted) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }
}