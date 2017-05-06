package stinc.male.sandbox.ratexecutor.tmp;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;
import javax.annotation.concurrent.ThreadSafe;
import stinc.male.sandbox.ratexecutor.NanosComparator;
import stinc.male.sandbox.ratexecutor.RateMeter;
import stinc.male.sandbox.ratexecutor.RateMeterConfig;
import stinc.male.sandbox.ratexecutor.RateMeterReading;
import stinc.male.sandbox.ratexecutor.RingBufferRateMeter;

@ThreadSafe
public final class RatMeX {//TODO use interface
  private final ExecutorService executor;
  private final ExecutorService submitter;//TODO use new Thread.start instead of ExecutorService

  public RatMeX(final ExecutorService executor) {
    this.executor = executor;
    submitter = Executors.newSingleThreadExecutor();
  }

  public final void submit(final Runnable task, final Duration samplesInterval, final RateMeterConfig rateMeterConfig, final ClosedInterval targetRateInterval) {
    submitter.submit(() -> {
      final long startNanos = System.nanoTime();
      final long samplesIntervalNanos = samplesInterval.toNanos();
      final long timeSensitivityNanos = rateMeterConfig.getTimeSensitivity().toNanos();
      final double targetTicksCountPerNano = targetRateInterval.getMean() / samplesIntervalNanos;
      final RateMeter rateMeter = new RingBufferRateMeter(startNanos, samplesInterval, rateMeterConfig);
      RateMeterReading rateMeterReading = new RateMeterReading();
      while(true) {
        final long currentNanos = System.nanoTime();
        final long passedNanos = currentNanos - startNanos;
        rateMeterReading = rateMeter.rate(currentNanos, rateMeterReading);
        final long ticksCount = rateMeterReading.getLongValue();
        final long ticksTotalCount = rateMeter.ticksTotalCount();
        final long targetTicksCount = (long)(targetTicksCountPerNano * passedNanos);
        final long targetTicksTotalCount = (long)(targetTicksCountPerNano * passedNanos);
        final long ticksCountDeviation = ticksCount - targetTicksCount;
        final long ticksTotalCountDeviation = ticksTotalCount - targetTicksTotalCount;
        System.out.println(rateMeterReading);
        if (ticksCountDeviation < 0) {//deficit
          for (int i = 0; i < -ticksCountDeviation; i++) {
            executor.execute(task);
          }
          rateMeter.tick(-ticksCountDeviation, currentNanos);
        } else {//surplus
          long sleepNanos = (long)((double)ticksCountDeviation / targetTicksCountPerNano);
          sleepNanos = Math.max(sleepNanos, timeSensitivityNanos);
          System.out.println("Sleep currentNanos=" + currentNanos + " durationNanos=" + sleepNanos);
          sleepUninterruptibly(currentNanos, sleepNanos);
        }
//        if (tNanos < startNanos + samplesIntervalNanos) {//we have just recently started
//          ;
//        } else {
//          ;
//        }
      }
    });
  }

  public final void shutdown() {//TODO wait and return future
    try {
      submitter.shutdownNow();
    } finally {
      executor.shutdownNow();
    }
  }

  private static final void sleepUninterruptibly(long tNanos, final long durationNanos) {
    final long targetTNanos = tNanos + durationNanos;
    long currentNanos = System.nanoTime();
    boolean interrupted = false;
    try {
      while (NanosComparator.compare(currentNanos, targetTNanos) < 0) {
        LockSupport.parkNanos(currentNanos - targetTNanos);
        if (Thread.interrupted()) {
          interrupted = true;
        }
        currentNanos = System.nanoTime();
      }
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }
}