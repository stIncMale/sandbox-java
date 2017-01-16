package stinc.male.sandbox.ratexecutor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.junit.Test;
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
import stinc.male.PerformanceTest;
import static java.lang.System.nanoTime;
import static java.time.Duration.ofMillis;
import static org.junit.Assert.assertEquals;
import static org.openjdk.jmh.runner.options.TimeValue.milliseconds;

@Category(PerformanceTest.class)
public class ScheduledThreadPoolExecutorMaxRateTest {
  private static final long NUMBER_OF_ACTIONS_PER_MEASUREMENT = 1_000_000;
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

  public ScheduledThreadPoolExecutorMaxRateTest() {
  }

  @Test
  public void referenceMeasurements() throws RunnerException {
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
              + ": max rate "
              + format(rate)
              + scoreUnits[1] + "s/" + scoreUnits[0]);
    });
  }

  @Test
  public void measurements() throws RunnerException {
    final AtomicLong counter = new AtomicLong();
    final ConcurrentRingBufferRateMeterConfig.Builder rmCfg = ConcurrentRingBufferRateMeter.defaultConfig()
            .toBuilder();
    rmCfg.setHl(2);
    rmCfg.setStrictTick(false);
    final RateMeter rm = new ConcurrentRingBufferRateMeter(
            nanoTime(),
            ofMillis(1),
            rmCfg.build());
    final int numberOfMeasurements = 50;
    final List<Double> measurements = new ArrayList<>(numberOfMeasurements);
    final ScheduledThreadPoolExecutor ex = new ScheduledThreadPoolExecutor(1);
    ex.scheduleAtFixedRate(() -> {
      rm.tick(1, nanoTime());
      counter.getAndIncrement();
    }, 0, 1, TimeUnit.NANOSECONDS);
    final long startNanos = nanoTime();
    final long startCount = counter.get();
    final long endCount = startCount + numberOfMeasurements * NUMBER_OF_ACTIONS_PER_MEASUREMENT;
    while(counter.get() < endCount) {
      measurements.add((double)rm.rate());
    }
    final long endNanos = nanoTime();
    final double rate = measurements.stream()
            .mapToDouble(Double::doubleValue)
            .sum() / measurements.size();
    final double averageRate = (double)ofMillis(1).toNanos() * (double)(endCount - startCount) / (endNanos - startNanos);
    assertEquals(0, rm.stats().failedAccuracyEventsCountForRate());
    assertEquals(0, rm.stats().failedAccuracyEventsCountForTick());
    System.out.println("scheduleAtFixedRate: measured rate "
            + format(rate)
            + "ops/ms"
            + ", measured average rate "
            + format(rm.rateAverage())
            + "ops/ms"
            + ", calculated average rate "
            + format(averageRate)
            + "ops/ms");
  }

  @Benchmark
  public void reference_scheduleAtFixedRate(final Reference_ScheduleAtFixedRate s) {
    final long counter = s.counter.get();
    final long targetCounter = counter + NUMBER_OF_ACTIONS_PER_MEASUREMENT;
    while (s.counter.get() < targetCounter);
  }

  @Benchmark
  public void reference_scheduleWithFixedDelay(final Reference_ScheduleWithFixedDelay s) {
    final long counter = s.counter.get();
    final long targetCounter = counter + NUMBER_OF_ACTIONS_PER_MEASUREMENT;
    while (s.counter.get() < targetCounter);
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

  private static final String format(final double v) {
    return String.format(Locale.ROOT, "%1.3f ", v);
  }
}