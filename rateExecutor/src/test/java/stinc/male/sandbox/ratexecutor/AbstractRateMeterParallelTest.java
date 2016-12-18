package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nullable;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public abstract class AbstractRateMeterParallelTest extends AbstractRateMeterTest {
  AbstractRateMeterParallelTest(final RateMeterCreator rateMeterCreator) {
    super(rateMeterCreator);
  }

  @Test
  public final void test() throws InterruptedException {
    for (int i = 0; i < 10; i++) {
      final Duration samplesInterval = Duration.ofMillis(ThreadLocalRandom.current().nextLong(400, 600));
      final TestParams tp = new TestParams(
          samplesInterval,
          ThreadLocalRandom.current().nextBoolean() ? Duration.ofNanos(1) : Duration.ofNanos((long)(ThreadLocalRandom.current().nextDouble(0.01, 0.1) * samplesInterval.toNanos())),
          ThreadLocalRandom.current().nextInt(5, 40),
          ThreadLocalRandom.current().nextInt(1, 5),
          Duration.ofMillis((long)(ThreadLocalRandom.current().nextDouble(1, 5) * samplesInterval.toMillis()))
      );
      doTest(tp);
    }
  }

  private final void doTest(final TestParams tp) throws InterruptedException {
    final long startNanos = System.nanoTime();
    final RateMeter rm = getRateMeterCreator().create(startNanos, tp.samplesInterval, RateMeterConfig.newBuilder().setTimeSensitivity(tp.timeSensitivity).build());
    final ScheduledExecutorService ses = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
    try {
      final AtomicBoolean onOffSwitch = new AtomicBoolean();
      final Collection<Ticker> tickers = new ArrayList<>();
      double rate = 0;
      for (int i = 1; i <= tp.numberOfTickersWithDifferentRate; i++) {
        for (int j = 0; j < tp.numberOfTickersWithSameRate; j++) {
          final Duration period = Duration.ofMillis(i * 10);
          tickers.add(new Ticker(rm, ses, period, onOffSwitch));
          rate += (double)tp.samplesInterval.toNanos() / period.toNanos();
        }
      }
      onOffSwitch.set(true);
      Thread.sleep(tp.samplesInterval.toMillis());
      Thread.sleep(tp.duration.toMillis());
      onOffSwitch.set(false);
      tickers.stream()
          .map(Ticker::failure)
          .filter(Objects::nonNull)
          .findAny()
          .ifPresent(failure -> {
            throw failure;
          });
      final long ticksTotalCount = tickers.stream()
          .mapToLong(Ticker::ticksTotalCount)
          .sum();
      final long lastNanos = tickers.stream()
          .mapToLong(Ticker::lastNanos)
          .max()
          .getAsLong();
      final double rateAverage = (double)ticksTotalCount * (double)tp.samplesInterval.toNanos() / (lastNanos - startNanos);
      assertEquals(tp.toString(), ticksTotalCount, rm.ticksTotalCount());
      if (tp.timeSensitivity.toNanos() == 1) {
        assertEquals(tp.toString(), lastNanos, rm.rightSamplesWindowBoundary());
        assertEquals(tp.toString(), rateAverage, rm.rateAverage(), rateAverage * 0.000000000001);
      } else {
        assertEquals(tp.toString(), lastNanos, rm.rightSamplesWindowBoundary(), 1.5 * tp.timeSensitivity.toNanos());
        assertEquals(tp.toString(), rateAverage, rm.rateAverage(), rateAverage * 0.075);
      }
      assertEquals(tp.toString(), rate, rm.rate(), rate * 0.075);
    } finally {
      ses.shutdownNow();
    }
  }

  private static final class Ticker {
    private final RateMeter rm;
    private final AtomicBoolean onOffSwitch;
    private final AtomicLong ticksTotal;
    private final AtomicLong lastNanos;
    @Nullable
    private volatile Throwable failure;

    private Ticker(final RateMeter rm, final ScheduledExecutorService ses, final Duration period, final AtomicBoolean onOffSwitch) {
      this.rm = rm;
      this.onOffSwitch = onOffSwitch;
      ticksTotal = new AtomicLong();
      lastNanos = new AtomicLong();
      ses.scheduleAtFixedRate(this::tick, 0, period.toMillis(), TimeUnit.MILLISECONDS);
    }

    private final void tick() {
      if (onOffSwitch.get()) {
        try {
          final long tNanos = System.nanoTime();
          rm.tick(1, tNanos);
          ticksTotal.incrementAndGet();
          lastNanos.updateAndGet(last -> tNanos > last ? tNanos : last);
        } catch (final Throwable e) {
          failure = e;
          throw e;
        }
      }
    }

    @Nullable
    final RuntimeException failure() {
      return failure == null ? null : new RuntimeException(failure);
    }

    final long lastNanos() {
      return lastNanos.get();
    }

    final long ticksTotalCount() {
      return ticksTotal.get();
    }
  }

  private static final class TestParams {
    final Duration samplesInterval;
    final Duration timeSensitivity;
    final int numberOfTickersWithDifferentRate;
    final int numberOfTickersWithSameRate;
    final Duration duration;

    TestParams(
        final Duration samplesInterval,
        final Duration timeSensitivity,
        final int numberOfTickersWithDifferentRate,
        final int numberOfTickersWithSameRate,
        final Duration duration) {
      this.samplesInterval = samplesInterval;
      this.timeSensitivity = timeSensitivity;
      this.numberOfTickersWithDifferentRate = numberOfTickersWithDifferentRate;
      this.numberOfTickersWithSameRate = numberOfTickersWithSameRate;
      this.duration = duration;
    }

    @Override
    public final String toString() {
      return getClass().getSimpleName()
          + "(samplesInterval=" + samplesInterval.toMillis()
          + ", timeSensitivity=" + timeSensitivity.toMillis()
          + ", numberOfTickersWithDifferentRate=" + numberOfTickersWithDifferentRate
          + ", numberOfTickersWithSameRate=" + numberOfTickersWithSameRate
          + ", duration=" + duration.toMillis()
          + ')';
    }
  }
}