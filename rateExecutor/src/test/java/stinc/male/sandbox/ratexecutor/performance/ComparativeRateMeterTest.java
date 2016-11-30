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
import stinc.male.sandbox.ratexecutor.AccurateRateMeter;
import stinc.male.sandbox.ratexecutor.AtomicLongTicksCounter;
import stinc.male.sandbox.ratexecutor.ConcurrentAccurateRateMeter;
import stinc.male.sandbox.ratexecutor.LongAdderTicksCounter;
import stinc.male.sandbox.ratexecutor.LongTicksCounter;
import stinc.male.sandbox.ratexecutor.RateMeter;
import stinc.male.sandbox.ratexecutor.RateMeterConfig;

@Category(PerformanceTest.class)
public class ComparativeRateMeterTest {
  private static final Duration samplesInterval = Duration.ofMillis(150);
  private static final boolean checkArguments = false;

  public ComparativeRateMeterTest() {
  }

  @Test
  public void benchmark() throws RunnerException {
    Options opt = new OptionsBuilder()
        .include(getClass().getName())
        .mode(Mode.Throughput)
        .timeUnit(TimeUnit.MILLISECONDS)
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

  @Benchmark
  public void empty_baseline() {
  }

  @Benchmark
  public void blackHoleConsume_baseline(final Blackhole bh) {
    bh.consume(1L);
  }

  @Benchmark
  public void tick_accurateRateMeter_longTicksCounter(final RateMeterContainer_ThreadScope state, final Blackhole bh) {
    tick(state.accurateRateMeter_longTicksCounter);
  }

  @Benchmark
  public void tick_accurateRateMeter_atomicLongTicksCounter(final RateMeterContainer_ThreadScope state, final Blackhole bh) {
    tick(state.accurateRateMeter_atomicLongTicksCounter);
  }

  @Benchmark
  public void tick_accurateRateMeter_longAdderTicksCounter(final RateMeterContainer_ThreadScope state, final Blackhole bh) {
    tick(state.accurateRateMeter_longAdderTicksCounter);
  }

  @Benchmark
  public void tick_concurrentAccurateRateMeter_longTicksCounter(final RateMeterContainer_ThreadScope state, final Blackhole bh) {
    tick(state.concurrentAccurateRateMeter_longTicksCounter);
  }

  @Benchmark
  public void tick_concurrentAccurateRateMeter_atomicLongTicksCounter(final RateMeterContainer_ThreadScope state, final Blackhole bh) {
    tick(state.concurrentAccurateRateMeter_atomicLongTicksCounter);
  }

  @Benchmark
  public void tick_concurrentAccurateRateMeter_longAdderTicksCounter(final RateMeterContainer_ThreadScope state, final Blackhole bh) {
    tick(state.concurrentAccurateRateMeter_longAdderTicksCounter);
  }

  @Benchmark
  public void tickAndRate_accurateRateMeter_longTicksCounter(final RateMeterContainer_ThreadScope state, final Blackhole bh) {
    tickAndRate(state.accurateRateMeter_longTicksCounter, bh);
  }

  @Benchmark
  public void tickAndRate_accurateRateMeter_atomicLongTicksCounter(final RateMeterContainer_ThreadScope state, final Blackhole bh) {
    tickAndRate(state.accurateRateMeter_atomicLongTicksCounter, bh);
  }

  @Benchmark
  public void tickAndRate_accurateRateMeter_longAdderTicksCounter(final RateMeterContainer_ThreadScope state, final Blackhole bh) {
    tickAndRate(state.accurateRateMeter_longAdderTicksCounter, bh);
  }

  @Benchmark
  public void tickAndRate_concurrentAccurateRateMeter_longTicksCounter(final RateMeterContainer_ThreadScope state, final Blackhole bh) {
    tickAndRate(state.concurrentAccurateRateMeter_longTicksCounter, bh);
  }

  @Benchmark
  public void tickAndRate_concurrentAccurateRateMeter_atomicLongTicksCounter(final RateMeterContainer_ThreadScope state, final Blackhole bh) {
    tickAndRate(state.concurrentAccurateRateMeter_atomicLongTicksCounter, bh);
  }

  @Benchmark
  public void tickAndRate_concurrentAccurateRateMeter_longAdderTicksCounter(final RateMeterContainer_ThreadScope state, final Blackhole bh) {
    tickAndRate(state.concurrentAccurateRateMeter_longAdderTicksCounter, bh);
  }

  private static final void tick(final RateMeter rm) {
    rm.tick(1, nanoTime());
  }

  private static final void tickAndRate(final RateMeter rm, final Blackhole bh) {
    rm.tick(1, nanoTime());
    bh.consume(rm.rate());
  }

  private static final long nanoTime() {
    return TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis());
  }

  @State(Scope.Thread)
  public static class RateMeterContainer_ThreadScope {
    AccurateRateMeter accurateRateMeter_longTicksCounter;
    AccurateRateMeter accurateRateMeter_atomicLongTicksCounter;
    AccurateRateMeter accurateRateMeter_longAdderTicksCounter;
    ConcurrentAccurateRateMeter concurrentAccurateRateMeter_longTicksCounter;
    ConcurrentAccurateRateMeter concurrentAccurateRateMeter_atomicLongTicksCounter;
    ConcurrentAccurateRateMeter concurrentAccurateRateMeter_longAdderTicksCounter;

    public RateMeterContainer_ThreadScope() {
    }

    @Setup
    public final void setup() {
      accurateRateMeter_longTicksCounter = new AccurateRateMeter(nanoTime(), samplesInterval,
          RateMeterConfig.newBuilder()
              .setCheckArguments(checkArguments)
              .setTicksCounterSupplier(LongTicksCounter::new)
              .build());
      accurateRateMeter_atomicLongTicksCounter = new AccurateRateMeter(nanoTime(), samplesInterval,
          RateMeterConfig.newBuilder()
              .setCheckArguments(checkArguments)
              .setTicksCounterSupplier(AtomicLongTicksCounter::new)
              .build());
      accurateRateMeter_longAdderTicksCounter = new AccurateRateMeter(nanoTime(), samplesInterval,
          RateMeterConfig.newBuilder()
              .setCheckArguments(checkArguments)
              .setTicksCounterSupplier(LongAdderTicksCounter::new)
              .build());
      concurrentAccurateRateMeter_longTicksCounter = new ConcurrentAccurateRateMeter(nanoTime(), samplesInterval,
          RateMeterConfig.newBuilder()
              .setCheckArguments(checkArguments)
              .setTicksCounterSupplier(LongTicksCounter::new)
              .build());
      concurrentAccurateRateMeter_atomicLongTicksCounter = new ConcurrentAccurateRateMeter(nanoTime(), samplesInterval,
          RateMeterConfig.newBuilder()
              .setCheckArguments(checkArguments)
              .setTicksCounterSupplier(AtomicLongTicksCounter::new)
              .build());
      concurrentAccurateRateMeter_longAdderTicksCounter = new ConcurrentAccurateRateMeter(nanoTime(), samplesInterval,
          RateMeterConfig.newBuilder()
              .setCheckArguments(checkArguments)
              .setTicksCounterSupplier(LongAdderTicksCounter::new)
              .build());
    }
  }
}