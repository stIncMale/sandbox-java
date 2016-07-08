package stinc.male.sandbox.ratexecutor.performance;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.GroupThreads;
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

public class ConcurrentRateSamplerTickTest {
  public ConcurrentRateSamplerTickTest() {
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

  @Group("g1")
  @GroupThreads(1)
  @Benchmark
  public void tick1(final RateSamplerContainer rateSamplerContainer, final Blackhole bh) throws Exception {
    final long tNanos;
    bh.consume(tNanos = TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis()));
    rateSamplerContainer.value.tick(1, tNanos);
  }

  @Group("g2")
  @GroupThreads(2)
  @Benchmark
  public void tick2(final RateSamplerContainer rateSamplerContainer, final Blackhole bh) throws Exception {
    tick1(rateSamplerContainer, bh);
  }

  @Group("g3")
  @GroupThreads(3)
  @Benchmark
  public void tick3(final RateSamplerContainer rateSamplerContainer, final Blackhole bh) throws Exception {
    tick1(rateSamplerContainer, bh);
  }

  @Group("g4")
  @GroupThreads(4)
  @Benchmark
  public void tick4(final RateSamplerContainer rateSamplerContainer, final Blackhole bh) throws Exception {
    tick1(rateSamplerContainer, bh);
  }

  @State(Scope.Group)
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