package stinc.male.sandbox.ratexecutor.performance;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openjdk.jmh.annotations.Benchmark;
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
import stinc.male.sandbox.ratexecutor.ConcurrentRateSampler;
import stinc.male.sandbox.ratexecutor.RateSampler;
import stinc.male.sandbox.ratexecutor.SimpleRateSampler;

@Category(PerformanceTest.class)
public class ComparativeRateSamplerTest {
  public ComparativeRateSamplerTest() {
  }

  @Test
  public void benchmark() throws RunnerException {
    Options opt = new OptionsBuilder()
        .include(getClass().getName())
        .mode(Mode.Throughput)
        .timeUnit(TimeUnit.MICROSECONDS)
        .warmupTime(TimeValue.seconds(1))
        .warmupIterations(1)
        .measurementTime(TimeValue.seconds(1))
        .measurementIterations(1)
        .syncIterations(true)
        .threads(1)
        .forks(1)
        .shouldFailOnError(true)
        .shouldDoGC(true)
        .timeout(TimeValue.seconds(10))
        .build();
    new Runner(opt).run();
  }

//  @Benchmark
//  public void emptyBaseline() throws Exception {
//  }
//
//  @Benchmark
//  public void tickAndCompoundBaseline(final Blackhole bh) throws Exception {
//    final long tNanos = TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis());
//    bh.consume(tNanos);
//  }

  @Benchmark
  public void simpleTick(final SimpleRateSamplerContainer rateSamplerContainer, final Blackhole bh) throws Exception {
    final long tNanos = TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis());
    rateSamplerContainer.value.tick(1, tNanos);
  }

  @Benchmark
  public void concurrentTick(final ConcurrentRateSamplerContainer rateSamplerContainer, final Blackhole bh) throws Exception {
    final long tNanos = TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis());
    rateSamplerContainer.value.tick(1, tNanos);
  }

  @Benchmark
  public void simpleCompound(final SimpleRateSamplerContainer rateSamplerContainer, final Blackhole bh) throws Exception {
    final long tNanos = TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis());
    rateSamplerContainer.value.tick(1, tNanos);
    bh.consume(rateSamplerContainer.value.rate());
  }

  @Benchmark
  public void concurrentCompound(final ConcurrentRateSamplerContainer rateSamplerContainer, final Blackhole bh) throws Exception {
    final long tNanos = TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis());
    rateSamplerContainer.value.tick(1, tNanos);
    bh.consume(rateSamplerContainer.value.rate());
  }

  @State(Scope.Thread)
  public static class SimpleRateSamplerContainer {
    volatile RateSampler value;

    public SimpleRateSamplerContainer() {
    }

    @Setup
    public final void setup() {
      value = new SimpleRateSampler(System.nanoTime(), Duration.ofMillis(150));
    }
  }

  @State(Scope.Thread)
  public static class ConcurrentRateSamplerContainer {
    volatile RateSampler value;

    public ConcurrentRateSamplerContainer() {
    }

    @Setup
    public final void setup() {
      value = new ConcurrentRateSampler(System.nanoTime(), Duration.ofMillis(150));
    }
  }
}