package stinc.male.sandbox.ratexecutor.tmp;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import stinc.male.sandbox.ratexecutor.RingBufferRateMeter;

@Disabled
public final class RatMeXTest {
  public RatMeXTest() {
  }

  @Test
  public final void test() throws Exception {
    final ExecutorService executor = Executors.newSingleThreadExecutor();
    final RatMeX ratmex = new RatMeX(executor);
    //    final ClosedInterval rateInterval = ClosedInterval.of(1000000, 0.01);
    final ClosedInterval rateInterval = ClosedInterval.of(3, 0.01);
    final Duration samplesInterval = Duration.ofMillis(1000);
    //    final Duration timeSensitivity = Duration.ofMillis(10);
    final Duration timeSensitivity = Duration.ofMillis(1000);
    final LongAdder counter = new LongAdder();
    final long durationMillis = 5000;
    final AtomicLong aStartMillis = new AtomicLong();
    final AtomicLong aStopMillis = new AtomicLong();
    ratmex.submit(
        () -> {
          final long currentMillis = System.currentTimeMillis();
          if (aStartMillis.get() == 0) {
            aStartMillis.compareAndSet(0, currentMillis);
          }
          counter.increment();
          aStopMillis.accumulateAndGet(currentMillis, Math::max);
        },
        samplesInterval,
        RingBufferRateMeter.defaultConfig()
            .toBuilder()
            .setTimeSensitivity(timeSensitivity)
            .build(),
        rateInterval);
    Thread.sleep(durationMillis);
    ratmex.shutdown();
    final long count = counter.longValue();
    final long durationMillisActual = aStopMillis.get() - aStartMillis.get();
    final double rateAverageCalculated = durationMillisActual == 0
        ? count
        : (double)(count * samplesInterval.toMillis()) / (double)durationMillisActual;
    System.out.println();
    System.out.println("rateInterval=" + rateInterval);
    System.out.println("samplesInterval=" + samplesInterval);
    System.out.println("timeSensitivity=" + timeSensitivity);
    System.out.println("durationMillis=" + durationMillis + ", durationMillisActual=" + durationMillisActual);
    System.out.println("count=" + count);
    System.out.println("rateAverageCalculated=" + rateAverageCalculated);
  }
}