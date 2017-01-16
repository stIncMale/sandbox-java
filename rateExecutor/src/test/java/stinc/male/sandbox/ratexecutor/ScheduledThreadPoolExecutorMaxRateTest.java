package stinc.male.sandbox.ratexecutor;

import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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
import static org.openjdk.jmh.runner.options.TimeValue.milliseconds;

@Category(PerformanceTest.class)
public class ScheduledThreadPoolExecutorMaxRateTest {
  private static final long NUMBER_OF_PERFORMED_ACTIONS = 1_000_000;
  private static final boolean SERVER = true;
  private static final boolean QUICK = true;
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
  public void test() throws RunnerException {
    final Collection<RunResult> results = new Runner(jmhOptionsBuilderSupplier.get()
            .mode(Mode.AverageTime)
            .timeUnit(TimeUnit.MILLISECONDS)
            .include(getClass().getName() + ".*schedule.*")
            .threads(1)
            .build())
            .run();
    System.out.println();
    System.out.println("Processed results");
    results.forEach(runResult -> {
      final Result<?> primaryResult = runResult.getPrimaryResult();
      final double score = primaryResult.getScore();
      final String scoreUnit = primaryResult.getScoreUnit();
      final double rate = (double)NUMBER_OF_PERFORMED_ACTIONS / score;
      final String description = runResult.getPrimaryResult().getLabel();
      System.out.println(description + ":\tmax rate " + String.format(Locale.ROOT, "\t%1.3f", rate)  + "\t(" + scoreUnit + ")⁻\u00B9");
    });
  }

  @Benchmark
  public void scheduleAtFixedRate(final ScheduleAtFixedRate s) {
    final long counter = s.counter.get();
    final long targetCounter = counter + NUMBER_OF_PERFORMED_ACTIONS;
    while (s.counter.get() < targetCounter);
  }

  @Benchmark
  public void scheduleWithFixedDelay(final ScheduleWithFixedDelay s) {
    final long counter = s.counter.get();
    final long targetCounter = counter + NUMBER_OF_PERFORMED_ACTIONS;
    while (s.counter.get() < targetCounter);
  }

  private static final void action() {
  }

  @State(Scope.Thread)
  public static class ScheduleAtFixedRate {
    LongAdderTicksCounter counter;
    ScheduledThreadPoolExecutor ex;

    public ScheduleAtFixedRate() {
    }

    @Setup(Level.Iteration)
    public final void setup() {
      counter = new LongAdderTicksCounter(0);
      ex = new ScheduledThreadPoolExecutor(1);
      ex.scheduleAtFixedRate(() -> counter.add(1), 0, 1, TimeUnit.NANOSECONDS);
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
      }
    }

    @TearDown(Level.Iteration)
    public final void tearDown() {
      ex.shutdownNow();
    }
  }

  @State(Scope.Thread)
  public static class ScheduleWithFixedDelay {
    LongAdderTicksCounter counter;
    ScheduledThreadPoolExecutor ex;

    public ScheduleWithFixedDelay() {
    }

    @Setup(Level.Iteration)
    public final void setup() {
      counter = new LongAdderTicksCounter(0);
      ex = new ScheduledThreadPoolExecutor(1);
      ex.scheduleWithFixedDelay(() -> counter.add(1), 0, 1, TimeUnit.NANOSECONDS);
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
      }
    }

    @TearDown(Level.Iteration)
    public final void tearDown() {
      ex.shutdownNow();
    }
  }
}