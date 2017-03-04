package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.junit.experimental.categories.Category;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import stinc.male.test.harness.ConcurrencyTest;
import static java.lang.System.nanoTime;
import static java.time.Duration.ofMillis;
import static org.junit.Assert.assertEquals;
import static org.openjdk.jmh.runner.options.TimeValue.milliseconds;

@Category(ConcurrencyTest.class)//todo do something with this?
public class ScheduledThreadPoolExecutorRateTest {
  private static final long NUMBER_OF_ACTIONS_PER_MEASUREMENT = 10_000;
  private static final boolean SERVER = true;
  private static final boolean QUICK = false;
  private static final Supplier<ChainedOptionsBuilder> jmhOptionsBuilderSupplier = () -> {
    final ChainedOptionsBuilder result = new OptionsBuilder()
        .jvmArgsPrepend(SERVER ? "-server" : "-client")
        .syncIterations(true)
        .shouldFailOnError(true)
        .shouldDoGC(true)
        .timeout(milliseconds(30_000));
    if (QUICK) {
      result.warmupTime(milliseconds(100))
          .warmupIterations(1)
          .measurementTime(milliseconds(100))
          .measurementIterations(1)
          .forks(1);
    } else {
      result.warmupTime(milliseconds(750))
          .warmupIterations(3)
          .measurementTime(milliseconds(1000))
          .measurementIterations(3)
          .forks(3);
    }
    return result;
  };

  public ScheduledThreadPoolExecutorRateTest() {
  }

//  @Test
  public final void reference() throws RunnerException {
    final Collection<RunResult> results = new Runner(jmhOptionsBuilderSupplier.get()
        .mode(Mode.AverageTime)
        .timeUnit(TimeUnit.MILLISECONDS)
        .include(getClass().getName() + ".*reference_schedule.*")
        .threads(1)
        .build())
        .run();
    System.out.println();
    System.out.println("Processed results");
    results.forEach(runResult -> {
      final Result<?> primaryResult = runResult.getPrimaryResult();
      final double score = primaryResult.getScore();
      final String scoreUnit = primaryResult.getScoreUnit();
      final String[] scoreUnits = scoreUnit.split("/");
      final double rate = (double) NUMBER_OF_ACTIONS_PER_MEASUREMENT / score;
      final String description = runResult.getPrimaryResult().getLabel();
      System.out.println(description
          + ": rate " + format(rate) + scoreUnits[1] + "s/" + scoreUnits[0]);
    });
  }

//  @Test
  public final void baseline_scheduleAtFixedRate() throws RunnerException {
    rate(ScheduleType.FIXED_RATE, true);
  }

//  @Test
  public final void baseline_scheduleWithFixedDelay() throws RunnerException {
    rate(ScheduleType.FIXED_DELAY, true);
  }

//  @Test
  public final void scheduleAtFixedRate() throws RunnerException {
    rate(ScheduleType.FIXED_RATE, false);
  }

//  @Test
  public final void scheduleWithFixedDelay() throws RunnerException {
    rate(ScheduleType.FIXED_DELAY, false);
  }

  private final void rate(final ScheduleType scheduleType, final boolean baseline) throws RunnerException {
    final AtomicLong counter = new AtomicLong();
    final RateMeter rm;
    {//setup
      final ConcurrentRingBufferRateMeterConfig.Builder rmCfg = ConcurrentRingBufferRateMeter.defaultConfig()
          .toBuilder();
      rmCfg.setHl(20);
      rmCfg.setStrictTick(false);
      rmCfg.setTimeSensitivity(Duration.of(200, ChronoUnit.MICROS));
      rm = new ConcurrentRingBufferRateMeter(
          nanoTime(),
          ofMillis(1),
          rmCfg.build());
      final ScheduledThreadPoolExecutor ex = new ScheduledThreadPoolExecutor(1);
      switch (scheduleType) {
        case FIXED_RATE: {
          ex.scheduleAtFixedRate(
              baseline
                  ? () -> counter.getAndIncrement()
                  : () -> {
                rm.tick(1, nanoTime());
                counter.getAndIncrement();
              },
              0, 1, TimeUnit.NANOSECONDS);
          break;
        }
        case FIXED_DELAY: {
          ex.scheduleWithFixedDelay(
              baseline
                  ? () -> counter.getAndIncrement()
                  : () -> {
                rm.tick(1, nanoTime());
                counter.getAndIncrement();
              },
              0, 1, TimeUnit.NANOSECONDS);
          break;
        }
        default: {
          throw new IllegalArgumentException(String.format("Unsupported %s=%s", "scheduleType", scheduleType));
        }
      }
    }
    final int numberOfMeasurements = 5000;
    final List<Double> measurements = new ArrayList<>(1000_000);
    {//warm up
      System.out.println("Warm up...");
      final long warmUpDurationNanos = ofMillis(2_000).toNanos();
      final long startNanos = nanoTime();
      while (nanoTime() - startNanos < warmUpDurationNanos) {
        measurements.add(baseline ? 0 : rm.rate(ofMillis(1)));
        if (measurements.size() > 100_000) {
          measurements.clear();
        }
      }
      measurements.clear();
      try {
        Thread.sleep(rm.getSamplesInterval().toMillis() + 1);
      } catch (final InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    {//measurement
      System.out.println("Measurement...");
      final long startNanos = nanoTime();
      final long startCount = counter.get();
      final long endCount = startCount + numberOfMeasurements * NUMBER_OF_ACTIONS_PER_MEASUREMENT;
      long count = counter.get();
      while (count < endCount) {
        if (count % 1000 == 0) {
          measurements.add(baseline ? 0 : rm.rate(ofMillis(1)));
        }
        count = counter.get();
      }
      final long endNanos = nanoTime();
      final double rate = measurements.stream()
          .mapToDouble(Double::doubleValue)
          .sum() / measurements.size();
      final double averageRate = (double) ofMillis(1).toNanos() * (double) (endCount - startCount) / (endNanos - startNanos);
      assertEquals(0d, rm.stats().failedAccuracyEventsCountForRate(), 0);
      assertEquals(0d, rm.stats().failedAccuracyEventsCountForTick(), 200);
      System.out.println(scheduleType.toString()
          + " measured rate " + format(rate) + "ops/ms"
          + ", measured average rate " + format(rm.rateAverage(ofMillis(1))) + "ops/ms"
          + ", calculated average rate " + format(averageRate) + "ops/ms");
    }
  }

  @Benchmark
  public void reference_scheduleAtFixedRate(final Reference_ScheduleAtFixedRate s) {
    final long counter = s.counter.get();
    final long targetCounter = counter + NUMBER_OF_ACTIONS_PER_MEASUREMENT;
    while (s.counter.get() < targetCounter) ;
  }

  @Benchmark
  public void reference_scheduleWithFixedDelay(final Reference_ScheduleWithFixedDelay s) {
    final long counter = s.counter.get();
    final long targetCounter = counter + NUMBER_OF_ACTIONS_PER_MEASUREMENT;
    while (s.counter.get() < targetCounter) ;
  }

  @State(Scope.Thread)
  public static class Reference_ScheduleAtFixedRate {
    AtomicLong counter;
    ScheduledThreadPoolExecutor ex;

    public Reference_ScheduleAtFixedRate() {
    }

    @Setup(Level.Trial)
    public final void setup() {
      counter = new AtomicLong(0);
      ex = new ScheduledThreadPoolExecutor(1);
      ex.scheduleAtFixedRate(() -> counter.getAndIncrement(), 0, 1, TimeUnit.NANOSECONDS);
    }

    @TearDown(Level.Trial)
    public final void tearDown() {
      ex.shutdownNow();
    }
  }

  @State(Scope.Thread)
  public static class Reference_ScheduleWithFixedDelay {
    AtomicLong counter;
    ScheduledThreadPoolExecutor ex;

    public Reference_ScheduleWithFixedDelay() {
    }

    @Setup(Level.Trial)
    public final void setup() {
      counter = new AtomicLong(0);
      ex = new ScheduledThreadPoolExecutor(1);
      ex.scheduleWithFixedDelay(() -> counter.getAndIncrement(), 0, 1, TimeUnit.NANOSECONDS);
    }

    @TearDown(Level.Trial)
    public final void tearDown() {
      ex.shutdownNow();
    }
  }

  private enum ScheduleType {
    FIXED_RATE("scheduleAtFixedRate"),
    FIXED_DELAY("scheduleWithFixedDelay");

    private final String description;

    ScheduleType(final String description) {
      this.description = description;
    }

    @Override
    public final String toString() {
      return description;
    }
  }

  private static final String format(final double v) {
    return String.format(Locale.ROOT, "%1.3f ", v);
  }
}