package stinc.male.sandbox.ratexecutor.tmp;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import org.junit.Test;
import stinc.male.sandbox.ratexecutor.RingBufferRateMeter;

public final class RatMeXTest {
  public RatMeXTest() {
  }

  @Test
  public final void test() throws Exception {
    final ExecutorService executor = Executors.newSingleThreadExecutor();
    final RatMeX ratmex = new RatMeX(executor);
      final ClosedInterval interval = new ClosedInterval(150, 160);
    final Duration samplesInterval = Duration.ofMillis(1000);
    final Duration timeSensitivity = Duration.ofMillis(200);
    final LongAdder counter = new LongAdder();
    final long durationMillis = 5000;
    final AtomicLong aStartMillis = new AtomicLong();
    final AtomicLong aStopMillis = new AtomicLong();
    ratmex.submit(
        () -> {
          final long currentMillis = System.currentTimeMillis();
          aStartMillis.compareAndSet(0, currentMillis);
          counter.increment();
          aStopMillis.accumulateAndGet(currentMillis, Math::max);
        },
        samplesInterval,
        RingBufferRateMeter.defaultConfig()
            .toBuilder()
            .setTimeSensitivity(timeSensitivity)
            .build(),
        interval);
    Thread.sleep(durationMillis);
    ratmex.shutdown();
    final long count = counter.longValue();
    final long actualDurationMillis = aStopMillis.get() - aStartMillis.get();
    final double averageRate = actualDurationMillis == 0
        ? count//there was only a single tick
        : (double)(count * samplesInterval.toMillis()) / actualDurationMillis;
    System.out.println("interval=" + interval);
    System.out.println("durationMillis=" + durationMillis + ", actualDurationMillis=" + actualDurationMillis);
    System.out.println("count=" + count);
    System.out.println("averageRate=" + averageRate);
  }
}