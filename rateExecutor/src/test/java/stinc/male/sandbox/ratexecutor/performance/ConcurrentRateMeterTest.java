package stinc.male.sandbox.ratexecutor.performance;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.GroupThreads;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import stinc.male.PerformanceTest;
import stinc.male.sandbox.ratexecutor.ConcurrentRateMeter;
import stinc.male.sandbox.ratexecutor.RateMeter;

@Category(PerformanceTest.class)
public class ConcurrentRateMeterTest {
  public ConcurrentRateMeterTest() {
  }

  @Test
  public void launchBenchmark() throws RunnerException {
    Options opt = new OptionsBuilder()
        .include(getClass().getName())
        .mode(Mode.Throughput)
        .timeUnit(TimeUnit.MICROSECONDS)
        .warmupTime(TimeValue.seconds(1))
        .warmupIterations(1)
        .measurementTime(TimeValue.seconds(1))
        .measurementIterations(1)
        .syncIterations(true)
        .threads(4)
        .forks(1)
        .shouldFailOnError(true)
        .shouldDoGC(true)
        .timeout(TimeValue.seconds(10))
        .build();
    new Runner(opt).run();
  }

//  @Benchmark
//  public void emptyBaseline() {
//  }
//
//  @Benchmark
//  public void tickAndCompoundBaseline(final Blackhole bh) {
//    final long tNanos = TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis());
//    bh.consume(tNanos);
//  }
//
//  @Benchmark
//  public void rateBaseline(final Blackhole bh) {
//    bh.consume(0);
//  }

  @Group("tick_1")
  @GroupThreads(1)
  @Benchmark
  public void tick_1(final ConcurrentRateMeterContainer rateMeterContainer, final Blackhole bh) {
    final long tNanos = TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis());
    rateMeterContainer.value.tick(1, tNanos);
  }

  @Group("tick_2")
  @GroupThreads(2)
  @Benchmark
  public void tick_2(final ConcurrentRateMeterContainer rateMeterContainer, final Blackhole bh) {
    final long tNanos = TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis());
    rateMeterContainer.value.tick(1, tNanos);
  }

  @Group("tick_4")
  @GroupThreads(4)
  @Benchmark
  public void tick_4(final ConcurrentRateMeterContainer rateMeterContainer, final Blackhole bh) {
    final long tNanos = TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis());
    rateMeterContainer.value.tick(1, tNanos);
  }

  @Group("compound_3_1")
  @GroupThreads(3)
  @Benchmark
  public void compoundTick_3_1(final ConcurrentRateMeterContainer rateMeterContainer, final Blackhole bh) {
    final long tNanos = TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis());
    bh.consume(tNanos);
    rateMeterContainer.value.tick(1, tNanos);
  }

  @Group("compound_3_1")
  @GroupThreads(1)
  @Benchmark
  public void compoundRate_3_1(final ConcurrentRateMeterContainer rateMeterContainer, final Blackhole bh) {
    bh.consume(rateMeterContainer.value.rate());
  }

  @Group("compound_2_2")
  @GroupThreads(2)
  @Benchmark
  public void compoundTick_2_2(final ConcurrentRateMeterContainer rateMeterContainer, final Blackhole bh) {
    final long tNanos = TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis());
    bh.consume(tNanos);
    rateMeterContainer.value.tick(1, tNanos);
  }

  @Group("compound_2_2")
  @GroupThreads(2)
  @Benchmark
  public void compoundRate_2_2(final ConcurrentRateMeterContainer rateMeterContainer, final Blackhole bh) {
    bh.consume(rateMeterContainer.value.rate());
  }

  @State(Scope.Group)
  public static class ConcurrentRateMeterContainer {
    volatile RateMeter value;

    public ConcurrentRateMeterContainer() {
    }

    @Setup
    public final void setup() {
      value = new ConcurrentRateMeter(System.nanoTime(), Duration.ofMillis(150));
    }
  }
}