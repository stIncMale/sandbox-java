package stinc.male.sandbox.ratexecutor.tmp;

import java.text.NumberFormat;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import stinc.male.sandbox.ratexecutor.ConcurrentRingBufferRateMeter;
import stinc.male.sandbox.ratexecutor.ConcurrentRingBufferRateMeterConfig;
import stinc.male.sandbox.ratexecutor.LongAdderTicksCounter;
import stinc.male.sandbox.ratexecutor.LongTicksCounter;
import stinc.male.sandbox.ratexecutor.ParkWaitStrategy;
import stinc.male.sandbox.ratexecutor.RateMeter;
import stinc.male.sandbox.ratexecutor.RateMeterReading;
import stinc.male.sandbox.ratexecutor.RingBufferRateMeter;
import stinc.male.sandbox.ratexecutor.StampedLockStrategy;
import static java.lang.Math.round;

@Disabled
public final class BatchingRateMeasuringExecutorTest {
  public BatchingRateMeasuringExecutorTest() {
  }

  private static final void nop() {
  }

  private final Map<Long, Runnable> batchCache = new HashMap<>();

  private final Runnable batch(final Runnable r, final long size) {
    final Runnable result;
    if (size > 1) {
      result = batchCache.computeIfAbsent(size, key -> () -> {
        for (long i = 0; i < key; i++) {
          r.run();
        }
      });
    } else {
      result = r;
    }
    return result;
  }

  private final void batchExecute(
      final Executor ex,
      final Runnable task,
      final long tasksCount,
      final long batchesCount,
      final RateMeter rateMeter,
      final long tNanos) {
    rateMeter.tick(tasksCount, tNanos);
    final long batchSize = tasksCount / batchesCount;
    if (batchSize > 1) {
      final Runnable baseBatch = batch(task, batchSize);
      final long remainder = tasksCount - batchesCount * batchSize;
      if (remainder == 0) {
        execute(ex, baseBatch, batchesCount);
      } else {//we need to evenly spread out the remainder
        assert remainder < batchesCount;
        final Runnable extendedBatch = batch(task, batchSize + 1);
        final boolean baseFirst = ThreadLocalRandom.current()
            .nextBoolean();
        if (baseFirst) {
          execute(ex, baseBatch, batchesCount - remainder);
          execute(ex, extendedBatch, remainder);
        } else {
          execute(ex, extendedBatch, remainder);
          execute(ex, baseBatch, batchesCount - remainder);
        }
      }
    } else {//batching is not appropriate
      execute(ex, task, tasksCount);
    }
  }

  private final void execute(
      final Executor ex,
      final Runnable task,
      final long tasksCount) {
    for (long i = 0; i < tasksCount; i++) {
      ex.execute(task);
    }
  }

  @Test
  public final void test() throws Exception {
    final int threadsCount = 2;
    final ScheduledExecutorService submitterEx = Executors.newScheduledThreadPool(1, new BoundedThreadFactory(1));
    submitterEx.submit(() -> {});
    final ExecutorService ex = Executors.newFixedThreadPool(threadsCount, new PrintThreadFactory());
    final CountDownLatch latch = new CountDownLatch(threadsCount);
    for (int i = 0; i < threadsCount; i++) {
      ex.submit(() -> {
        try {
          latch.await();
        } catch (final InterruptedException e) {
          Thread.currentThread()
              .interrupt();
        }
      });
      latch.countDown();
    }
    System.out.println("Threads have been started");
    final AtomicLong submitDurationNanos = new AtomicLong();
    final Duration timeSensitivity = Duration.ofNanos(5_000);
    final Duration samplesInterval = Duration.ofNanos(10_000);
    final long targetRatePerSecond = 5_000_000;
    final long targetSubmitsTotal = targetRatePerSecond * 5L;
    final ClosedInterval targetSubmits = ClosedInterval.of(
        round((double)targetRatePerSecond * (double)samplesInterval.toNanos() / (double)Duration.ofSeconds(1)
            .toNanos()),
        0.05);
    final long startNanos = System.nanoTime();
    final RateMeter submitterRateMeter = new RingBufferRateMeter(
        startNanos,
        samplesInterval,
        RingBufferRateMeter.defaultConfig()
            .toBuilder()
            .setTicksCounterSupplier(LongTicksCounter::new)
            .setHl(2)
            .setTimeSensitivity(timeSensitivity)
            .setCollectStats(false)
            .setMaxTicksCountAttempts(1)
            .build());
    final ConcurrentRingBufferRateMeterConfig.Builder concurrentRmCfgBuilder = ConcurrentRingBufferRateMeter.defaultConfig()
        .toBuilder()
        .setStrictTick(false)
        .setLockStrategySupplier(StampedLockStrategy::new)
        .setWaitStrategySupplier(ParkWaitStrategy::instance);
    concurrentRmCfgBuilder.setTicksCounterSupplier(LongAdderTicksCounter::new)
        .setHl(3)
        .setTimeSensitivity(timeSensitivity)
        .setCollectStats(true)
        .setMaxTicksCountAttempts(5);
    final RateMeter completionRateMeter = new ConcurrentRingBufferRateMeter(startNanos, samplesInterval, concurrentRmCfgBuilder.build());
    final LongAdder globalCompleteCounter = new LongAdder();
    final Supplier<Runnable> submitterSupplier = () -> new Runnable() {
      private long submitsCounter;
      private final Runnable task;
      private final RateMeterReading submitterReading;
      private final RateMeterReading completionReading;
      private long prevTNanos;
      private final long samplesIntervalNanos;

      {
        submitsCounter = 0;
        task = new RateMeterAwareRunnable(() -> {
          globalCompleteCounter.increment();
          ThreadLocalRandom.current()
              .nextGaussian();
        }, completionRateMeter);
        submitterReading = new RateMeterReading();
        completionReading = new RateMeterReading();
        samplesIntervalNanos = submitterRateMeter.getSamplesInterval()
            .toNanos();
        prevTNanos = startNanos;
      }

      @Override
      public final void run() {
        final long tNanos = System.nanoTime();
        final long submitsRequired = submitsRequired(tNanos);
        submitsCounter += submitsRequired;
        batchExecute(ex, task, submitsRequired, threadsCount, submitterRateMeter, tNanos);
        if (submitsRequired > 0) {
          prevTNanos = tNanos;
        }
        if (submitsCounter >= targetSubmitsTotal) {
          submitDurationNanos.set(tNanos - startNanos);
          System.out.println(Duration.ofNanos(submitDurationNanos.get()));
          throw new CancellationException();
        }
      }

      private final long submitsRequired(final long tNanos) {
        final double measuredAverageSubmits = submitterRateMeter.rateAverage();
        final double deltaAverage = measuredAverageSubmits - targetSubmits.getMean();
        submitterRateMeter.rate(tNanos, submitterReading);
        completionRateMeter.rate(tNanos, completionReading);
        final long measuredSubmits = submitterReading.getLongValue();
        final double ratio = (tNanos - prevTNanos) / (double)samplesIntervalNanos;
        final long result;
        if (measuredSubmits < targetSubmits.getMean()) {//as expected; the interval (prevTNanos; tNanos] is fresh and we are deciding submits for it
          if (deltaAverage < 0) {//too slow in average; submit as many as possible but not exceed max
            result = round(ratio * targetSubmits.getMax());
//            System.out.println("1 " + result +
//                ", passedT(ms)=" + Duration.ofNanos(tNanos - startNanos)
//                .toMillis() +
//                ", deltaT(ms)=" + Duration.ofNanos(tNanos - prevTNanos)
//                .toMillis() +
//                ", measuredSubmits=" + measuredSubmits +
//                ", ratio=" + ratio);
          } else {//too fast in average; submit as few as possible to satisfy the min
            result = round(ratio * targetSubmits.getMin());
//            System.out.println("2 " + result);
          }
        } else {//too fast
          if (deltaAverage < 0) {//too slow in average; submit as many as possible but not exceed max
            result = round(ratio * targetSubmits.getMax());
//            System.out.println("3 " + result);
          } else {//too fast in average; do not submit anything
            result = 0;
//            System.out.println("4 " + result);
          }
        }
//        if (measuredSubmits < targetSubmits.getMean()) {//as expected; the interval (prevTNanos; tNanos] is fresh and we are deciding submits for it
//          if (deltaAverage < 0) {//too slow in average; submit as many as possible but not exceed max
//            result = measuredSubmits < targetSubmits.getMax()
//                ? round(ratio * (targetSubmits.getMax() - measuredSubmits))
//                : 0;
//            System.out.println("1 " + result +
//                ", passedT(ms)=" + Duration.ofNanos(tNanos - startNanos)
//                .toMillis() +
//                ", deltaT(ms)=" + Duration.ofNanos(tNanos - prevTNanos)
//                .toMillis() +
//                ", measuredSubmits=" + measuredSubmits +
//                ", ratio=" + ratio);
//          } else {//too fast in average; submit as few as possible to satisfy the min
//            result = measuredSubmits < targetSubmits.getMin()
//                ? round(ratio * (targetSubmits.getMin() - measuredSubmits))
//                : 0;
//            System.out.println("2 " + result);
//          }
//        } else {//too fast
//          if (deltaAverage < 0) {//too slow in average; submit as many as possible but not exceed max
//            result = measuredSubmits < targetSubmits.getMax()
//                ? round(ratio * (targetSubmits.getMax() - measuredSubmits))
//                : 0;
//            System.out.println("3 " + result);
//          } else {//too fast in average; do not submit anything
//            result = 0;
//            System.out.println("4 " + result);
//          }
//        }
        return result;
      }
    };
    final Future<?> submitting = submitterEx.scheduleAtFixedRate(
        submitterSupplier.get(),
        0,
        submitterRateMeter.getTimeSensitivity()
            .toNanos(),
        TimeUnit.NANOSECONDS);
    try {
      submitting.get();
    } catch (final ExecutionException e) {
      if (!(e.getCause() instanceof CancellationException)) {
        throw e;
      }
    }
    {
      Locale.setDefault(Locale.ROOT);
      final NumberFormat format = NumberFormat.getIntegerInstance();
      format.setGroupingUsed(true);
      System.out.println("submitterRateMeter.ticksTotalCount=" + format.format(submitterRateMeter.ticksTotalCount()) +
          ", submitterRateMeter.rateAverage=" + format.format(submitterRateMeter.rateAverage(Duration.ofSeconds(1))) +
          ", submitterRateMeter.rate=" + format.format(submitterRateMeter.rate(Duration.ofSeconds(1))));
      System.out.println("completionRateMeter.ticksTotalCount=" + format.format(completionRateMeter.ticksTotalCount()) +
          ", completionRateMeter.rateAverage=" + format.format((completionRateMeter.rateAverage(Duration.ofSeconds(1)))) +
          ", completionRateMeter.rate=" + format.format(completionRateMeter.rate(Duration.ofSeconds(1))));
      System.out.println("globalCompleteCounter=" + format.format(globalCompleteCounter) +
          ", globalCompleteCounter/submitDurationNanos=" + format.format((double)globalCompleteCounter.sum() /
          ((double)submitDurationNanos.get() / (double)Duration.ofSeconds(1)
              .toNanos())));
      System.out.println("targetRatePerSecond=" + format.format(targetRatePerSecond) +
          ", targetSubmits=" + targetSubmits);
      System.out.println("failedAccuracyEventsCountForTick=" + submitterRateMeter.stats()
          .failedAccuracyEventsCountForTick() +
          ", failedAccuracyEventsCountForRate=" + submitterRateMeter.stats()
          .failedAccuracyEventsCountForRate());
      System.out.println("batchCache.size=" + batchCache.size());
    }
  }

  private static final class BoundedThreadFactory implements ThreadFactory {
    private final int max;
    private final AtomicInteger counter;

    private BoundedThreadFactory(final int max) {
      this.max = max;
      counter = new AtomicInteger(0);
    }

    @Nullable
    @Override
    public final Thread newThread(final Runnable r) {
      @Nullable final Thread result;
      final int currentCount = counter.get();
      if (currentCount < max && counter.compareAndSet(currentCount, currentCount + 1)) {//omit CAS when possible (similar to DCL idiom)
        result = new Thread(r);
      } else {
        result = null;
      }
      System.out.println(result);
      return result;
    }
  }

  private static final class PrintThreadFactory implements ThreadFactory {
    private PrintThreadFactory() {
    }

    @Override
    public final Thread newThread(final Runnable r) {
      final Thread result = new Thread(r);
      System.out.println(result);
      return result;
    }
  }

  private static final class RateMeterAwareRunnable implements Runnable {
    private final Runnable r;
    private final RateMeter rateMeter;

    private RateMeterAwareRunnable(final Runnable r, final RateMeter rateMeter) {
      this.r = r;
      this.rateMeter = rateMeter;
    }

    @Override
    public final void run() {
      r.run();
      rateMeter.tick(1, System.nanoTime());
    }
  }
}