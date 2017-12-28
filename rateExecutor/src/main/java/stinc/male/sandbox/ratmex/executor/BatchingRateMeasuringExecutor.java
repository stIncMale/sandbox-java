package stinc.male.sandbox.ratmex.executor;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import stinc.male.sandbox.ratmex.meter.RateMeter;
import static stinc.male.sandbox.ratmex.util.internal.Preconditions.checkNotNull;

@ThreadSafe
public final class BatchingRateMeasuringExecutor implements RateMeasuringExecutor {
  private final ScheduledExecutorService submitter;
  private final ExecutorService worker;

  public BatchingRateMeasuringExecutor(final ExecutorService worker, @Nullable final ThreadFactory submitterThreadFactory) {
    checkNotNull(worker, "worker");
    this.worker = worker;
    submitter = Executors.newScheduledThreadPool(1, new BoundedThreadFactory(submitterThreadFactory, 1));
  }

  //  public final void submit(final Runnable task, final Duration samplesInterval, final RateMeterConfig rateMeterConfig, final Rate targetRateInterval) {
  //    submitter.submit(() -> {
  //      final long startNanos = System.nanoTime();
  //      final long samplesIntervalNanos = samplesInterval.toNanos();
  //      final long timeSensitivityNanos = rateMeterConfig.getTimeSensitivity().toNanos();
  //      final double targetTicksCountPerNano = targetRateInterval.getMean() / samplesIntervalNanos;
  //      final double maxTicksCountPerNano = targetRateInterval.getMax() / samplesIntervalNanos;
  //      final double minTicksCountPerNano = targetRateInterval.getMin() / samplesIntervalNanos;
  //      final RateMeter rateMeter = new RingBufferRateMeter(startNanos, samplesInterval, rateMeterConfig);
  //      RateMeterReading rateMeterReading = new RateMeterReading();
  //      System.out.println("Before the cycle targetTicksCountPerNano=" + targetTicksCountPerNano);
  //      while (!Thread.currentThread().isInterrupted()) {
  //        final long currentNanos = System.nanoTime();
  //        final long passedNanos = currentNanos - startNanos;
  //        rateMeterReading = rateMeter.rate(currentNanos, rateMeterReading);
  //        final long ticksTotalCount = rateMeter.ticksTotalCount();
  //        final long targetTicksTotalCount = (long) (targetTicksCountPerNano * passedNanos);
  //        final long ticksTotalCountDeviation = ticksTotalCount - targetTicksTotalCount;//VAKOTODO deviation must be calculated locally, not globally
  //        if (ticksTotalCountDeviation < 0 && rateMeterReading.getValueLong() < targetRateInterval.getMax()) {//deficit and we still have a budget
  //          final long affordableTicksCount = Math.min(-ticksTotalCountDeviation, targetRateInterval.getMax() - rateMeterReading.getValueLong());
  //          for (int i = 0; i < affordableTicksCount; i++) {
  //            executor.execute(task);
  //          }
  //          rateMeter.tick(affordableTicksCount, currentNanos);
  //        }
  //
  ////        else if (ticksTotalCountDeviation < 0) {//deficit that can not be covered right now
  ////          try {
  ////            sleep(currentNanos, timeSensitivityNanos);
  ////          } catch (final InterruptedException e) {
  ////            break;
  ////          }
  ////        } else {//surplus
  ////          long sleepNanos = (long)((double)ticksTotalCountDeviation / targetTicksCountPerNano);
  ////          sleepNanos = Math.max(sleepNanos, timeSensitivityNanos);
  ////          sleepUninterruptibly(currentNanos, sleepNanos);
  ////        }
  //      }
  //      System.out.println("After the cycle rateAverageMeasured=" + rateMeter.rateAverage() + ", durationMillisMeasured=" + Duration.ofNanos(rateMeter.rightSamplesWindowBoundary() - startNanos).toMillis());
  //    });
  //  }

  @Override
  public final Future<?> submit(final Runnable task, final RateMeter rateMeter, final Rate rate, final Duration delay) {
    return null;
  }

  @Override
  public void shutdown(boolean shutdownWorker) {
  }
}