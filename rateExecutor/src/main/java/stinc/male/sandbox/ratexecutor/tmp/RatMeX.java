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
      final double maxTicksCountPerNano = targetRateInterval.getRight() / samplesIntervalNanos;
      final double minTicksCountPerNano = targetRateInterval.getLeft() / samplesIntervalNanos;
      final RateMeter rateMeter = new RingBufferRateMeter(startNanos, samplesInterval, rateMeterConfig);
      RateMeterReading rateMeterReading = new RateMeterReading();
      System.out.println("Before the cycle targetTicksCountPerNano=" + targetTicksCountPerNano);
      while (!Thread.currentThread().isInterrupted()) {
        final long currentNanos = System.nanoTime();
        final long passedNanos = currentNanos - startNanos;
        println("Iteration currentNanos=" + currentNanos + " passedNanos=" + passedNanos);
        rateMeterReading = rateMeter.rate(currentNanos, rateMeterReading);
        final long ticksTotalCount = rateMeter.ticksTotalCount();
        final long targetTicksTotalCount = (long) (targetTicksCountPerNano * passedNanos);
        final long ticksTotalCountDeviation = ticksTotalCount - targetTicksTotalCount;
        println("\t" + rateMeterReading + ", ticksTotalCountDeviation=" + ticksTotalCountDeviation);
        if (ticksTotalCountDeviation < 0 && rateMeterReading.getLongValue() < targetRateInterval.getRight()) {//deficit and we still have a budget
          final long requiredTicksCount = Math.min(-ticksTotalCountDeviation, targetRateInterval.getRight() - rateMeterReading.getLongValue());
          for (int i = 0; i < requiredTicksCount; i++) {
            executor.execute(task);
          }
          println("\ttick(" + requiredTicksCount + ", " + currentNanos + ")");
          rateMeter.tick(requiredTicksCount, currentNanos);
        }
        try {
          sleep(currentNanos, timeSensitivityNanos);
        } catch (final InterruptedException e) {
          break;
        }

//        else if (ticksTotalCountDeviation < 0) {//deficit that can not be covered right now
//          try {
//            sleep(currentNanos, timeSensitivityNanos);
//          } catch (final InterruptedException e) {
//            break;
//          }
//        } else {//surplus
//          long sleepNanos = (long)((double)ticksTotalCountDeviation / targetTicksCountPerNano);
//          sleepNanos = Math.max(sleepNanos, timeSensitivityNanos);
//          sleepUninterruptibly(currentNanos, sleepNanos);
//        }
      }
      System.out.println("After the cycle rateAverage=" + rateMeter.rateAverage());
    });
  }

  public final void shutdown() {//TODO wait and return future
    try {
      submitter.shutdownNow();
    } finally {
      executor.shutdownNow();
    }
    try {
      Thread.sleep(1000);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private static final void sleep(long tNanos, final long durationNanos) throws InterruptedException {
    println("\tsleep tNanos=" + tNanos + " durationNanos=" + durationNanos);
    final long targetTNanos = tNanos + durationNanos;
    long currentNanos = System.nanoTime();
    while (NanosComparator.compare(currentNanos, targetTNanos) < 0 && !Thread.currentThread().isInterrupted()) {
      LockSupport.parkNanos(currentNanos - targetTNanos);
      currentNanos = System.nanoTime();
    }
  }

  private static final void sleepUninterruptibly(long tNanos, final long durationNanos) {
    println("\tsleep tNanos=" + tNanos + " durationNanos=" + durationNanos);
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

  private static final void println(final String s) {
    System.out.println(s);
  }
}