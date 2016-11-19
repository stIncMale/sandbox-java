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
import stinc.male.sandbox.ratexecutor.RateSampler;
import stinc.male.sandbox.ratexecutor.SimpleRateSampler;

public class SimpleRateSamplerTickTest {
  public SimpleRateSamplerTickTest() {
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
  public void tickBaseline(final Blackhole bh) throws Exception {
    bh.consume(TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis()));
  }

  @Benchmark
  public void tick(final RateSamplerContainer rateSamplerContainer, final Blackhole bh) throws Exception {
    final long tNanos;
    bh.consume(tNanos = TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis()));
    rateSamplerContainer.value.tick(1, tNanos);
  }

  @State(Scope.Thread)
  public static class RateSamplerContainer {
    volatile RateSampler value;

    public RateSamplerContainer() {
    }

    @Setup
    public final void setup() {
      value = new SimpleRateSampler(System.nanoTime(), Duration.ofMillis(150));
    }
  }
}