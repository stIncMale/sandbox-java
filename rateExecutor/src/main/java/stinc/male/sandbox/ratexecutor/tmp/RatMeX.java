package stinc.male.sandbox.ratexecutor.tmp;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;
import javax.annotation.concurrent.ThreadSafe;
import stinc.male.sandbox.ratexecutor.RateMeter;

@ThreadSafe
public final class RatMeX {
  private final Executor executor;
  private final RateMeter rateMeter;
  private final ExecutorService submitter;

  public RatMeX(final Executor executor, final RateMeter rateMeter) {
    this.executor = executor;
    this.rateMeter = rateMeter;
    submitter = Executors.newSingleThreadExecutor();
  }

  public final void submit(final Runnable task, final ClosedInterval targetRateInterval) {
    submitter.submit(() -> {
      while(true) {
        long tNanos = System.nanoTime();
        final double currentRate = rateMeter.rate(tNanos);
        final double rateDelta = currentRate - targetRateInterval.getMean();
        if (rateDelta < 0) {
          for (long i = Math.round(rateDelta); i < 0; i++) {
            rateMeter.tick(1, tNanos);
            executor.execute(task);
          }
        } else if (rateDelta == 0) {
          sleep(tNanos, rateMeter.getSamplesInterval().toNanos() / 20d);
        } else {
          sleep(tNanos, rateDelta / (targetRateInterval.getMean() / rateMeter.getSamplesInterval().toNanos()));
        }
      }
    });
  }

  private static final void sleep(long tNanos, final double durationNanos) {//if one thread waits - all thread wait
    System.out.println(durationNanos);
    final double targetTNanos = tNanos + durationNanos;
    boolean interrupted = false;
    try {
      while (tNanos < targetTNanos) {
        LockSupport.parkNanos((long) durationNanos);
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