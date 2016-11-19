package stinc.male.sandbox.ratexecutor.performance;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import stinc.male.sandbox.ratexecutor.ConcurrentRateSampler;
import stinc.male.sandbox.ratexecutor.RateSampler;

public class SimpleRateSamplerCompoundPerformanceTest {
  public SimpleRateSamplerCompoundPerformanceTest() {
  }

  @Test
  public void launchBenchmark() throws Exception {
    Options opt = new OptionsBuilder()
        .include(getClass().getName())
        .mode(Mode.Throughput)
        .timeUnit(TimeUnit.MILLISECONDS)
        .warmupTime(TimeValue.seconds(2))
        .warmupIterations(3)
        .measurementTime(TimeValue.seconds(2))
        .measurementIterations(3)
        .syncIterations(true)
        .threads(4)
        .forks(2)
        .shouldFailOnError(true)
        .shouldDoGC(true)
        .timeout(TimeValue.seconds(3))
        .build();
    new Runner(opt).run();
  }

  @Benchmark
  public void tickRateBaseline(final Blackhole blackhole) throws Exception {
    final long tNanos = TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis());
    blackhole.consume(tNanos);
    blackhole.consume(0);
  }

  @Benchmark
  public void tickRate(final RateSamplerContainer rateSamplerContainer, final Blackhole bh) throws Exception {
    final long tNanos = TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis());
    bh.consume(tNanos);
    rateSamplerContainer.value.tick(1, tNanos);
    bh.consume(rateSamplerContainer.value.rate());
  }

  @State(Scope.Thread)
  public static class RateSamplerContainer {
    volatile RateSampler value;

    public RateSamplerContainer() {
    }

    @Setup
    public final void setup() {
      value = new ConcurrentRateSampler(System.nanoTime(), Duration.ofMillis(150));
    }
  }
}