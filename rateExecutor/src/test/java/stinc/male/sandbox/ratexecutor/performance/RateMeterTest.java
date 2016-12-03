package stinc.male.sandbox.ratexecutor.performance;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.GroupThreads;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
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
public class RateMeterTest {
  private static final Duration samplesInterval = Duration.ofMillis(1);
  private static final boolean millisInsteadOfNanos = false;
  private static final boolean quickRun = true;
  private static final Supplier<ChainedOptionsBuilder> jmhOptionsBuilderSupplier = () -> {
    final ChainedOptionsBuilder result = new OptionsBuilder().mode(Mode.Throughput)
        .timeUnit(TimeUnit.MILLISECONDS)
        .syncIterations(true)
        .shouldFailOnError(true)
        .shouldDoGC(true)
        .timeout(TimeValue.seconds(30));
    if (quickRun) {
      result.warmupTime(TimeValue.seconds(1))
          .warmupIterations(0)
          .measurementTime(TimeValue.seconds(1))
          .measurementIterations(1)
          .forks(1);
    } else {
      result.warmupTime(TimeValue.seconds(2))
          .warmupIterations(3)
          .measurementTime(TimeValue.seconds(2))
          .measurementIterations(3)
          .forks(3)
          .include(RateMeterTest.class.getName() + ".*_baseline");
    }
    return result;
  };
  private static final Supplier<RateMeterConfig.Builder> rateMeterConfigBuilderSuppplier = () -> RateMeterConfig.newBuilder()
      .setCheckArguments(false);

  public RateMeterTest() {
  }

  @Test
  public void serialTest() throws RunnerException {
    new Runner(jmhOptionsBuilderSupplier.get()
        .include(getClass().getName() + ".*serial_.*")
        .threads(1)
        .build())
        .run();
  }

  @Test
  public void parallelTest() throws RunnerException {
    new Runner(jmhOptionsBuilderSupplier.get()
        .include(getClass().getName() + ".*parallel\\$.*")
        .threads(4)
        .build())
        .run();
  }

  @Benchmark
  public void empty_baseline() {
  }

  @Benchmark
  public void nanoTime_baseline(final Blackhole bh) {
    bh.consume(nanoTime());
  }

  @Benchmark
  public void intCounter_baseline(IntCounter_ThreadScope counter, final Blackhole bh) {
    counter.v++;
    bh.consume(counter.v % 10);
  }

  @Benchmark
  public void serial_tick_accurateRateMeter_longTicksCounter(final RateMeterContainer_ThreadScope state) {
    tick(state.accurateRateMeter_longTicksCounter);
  }

  @Benchmark
  public void serial_tick_accurateRateMeter_atomicLongTicksCounter(final RateMeterContainer_ThreadScope state) {
    tick(state.accurateRateMeter_atomicLongTicksCounter);
    state.all.add(state.accurateRateMeter_atomicLongTicksCounter.rate(nanoTime()));
//    if (state.accurateRateMeter_atomicLongTicksCounter.ticksTotalCount() % 50_000 == 0) {
//      System.out.println(state.accurateRateMeter_atomicLongTicksCounter.rate(nanoTime(), Duration.ofMillis(1)));
//    }//TODO
  }

  @Benchmark
  public void serial_tick_accurateRateMeter_longAdderTicksCounter(final RateMeterContainer_ThreadScope state) {
    tick(state.accurateRateMeter_longAdderTicksCounter);
  }

  @Benchmark
  public void serial_tick_concurrentAccurateRateMeter_longTicksCounter(final RateMeterContainer_ThreadScope state) {
    tick(state.concurrentAccurateRateMeter_longTicksCounter);
  }

  @Benchmark
  public void serial_tick_concurrentAccurateRateMeter_atomicLongTicksCounter(final RateMeterContainer_ThreadScope state) {
    tick(state.concurrentAccurateRateMeter_atomicLongTicksCounter);
  }

  @Benchmark
  public void serial_tick_concurrentAccurateRateMeter_longAdderTicksCounter(final RateMeterContainer_ThreadScope state) {
    tick(state.concurrentAccurateRateMeter_longAdderTicksCounter);
  }

  @Benchmark
  public void serial_tick$100rate$1_accurateRateMeter_longTicksCounter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.accurateRateMeter_longTicksCounter, bh, counter.v++, 100);
  }

  @Benchmark
  public void serial_tick$100rate$1_accurateRateMeter_atomicLongTicksCounter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.accurateRateMeter_atomicLongTicksCounter, bh, counter.v++, 100);
  }

  @Benchmark
  public void serial_tick$100rate$1_accurateRateMeter_longAdderTicksCounter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.accurateRateMeter_longAdderTicksCounter, bh, counter.v++, 100);
  }

  @Benchmark
  public void serial_tick$100rate$1_concurrentAccurateRateMeter_longTicksCounter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentAccurateRateMeter_longTicksCounter, bh, counter.v++, 100);
  }

  @Benchmark
  public void serial_tick$100rate$1_concurrentAccurateRateMeter_atomicLongTicksCounter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentAccurateRateMeter_atomicLongTicksCounter, bh, counter.v++, 100);
  }

  @Benchmark
  public void serial_tick$100rate$1_concurrentAccurateRateMeter_longAdderTicksCounter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentAccurateRateMeter_longAdderTicksCounter, bh, counter.v++, 100);
  }

  @Benchmark
  public void serial_tick$10rate$1_accurateRateMeter_longTicksCounter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.accurateRateMeter_longTicksCounter, bh, counter.v++, 10);
  }

  @Benchmark
  public void serial_tick$10rate$1_accurateRateMeter_atomicLongTicksCounter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.accurateRateMeter_atomicLongTicksCounter, bh, counter.v++, 10);
  }

  @Benchmark
  public void serial_tick$10rate$1_accurateRateMeter_longAdderTicksCounter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.accurateRateMeter_longAdderTicksCounter, bh, counter.v++, 10);
  }

  @Benchmark
  public void serial_tick$10rate$1_concurrentAccurateRateMeter_longTicksCounter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentAccurateRateMeter_longTicksCounter, bh, counter.v++, 10);
  }

  @Benchmark
  public void serial_tick$10rate$1_concurrentAccurateRateMeter_atomicLongTicksCounter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentAccurateRateMeter_atomicLongTicksCounter, bh, counter.v++, 10);
  }

  @Benchmark
  public void serial_tick$10rate$1_concurrentAccurateRateMeter_longAdderTicksCounter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentAccurateRateMeter_longAdderTicksCounter, bh, counter.v++, 10);
  }

  @Benchmark
  @Group("parallel_tick_concurrentAccurateRateMeter_atomicLongTicksCounter")
  @GroupThreads(1)
  public void parallel$1_tick_concurrentAccurateRateMeter_atomicLongTicksCounter(final RateMeterContainer_GroupScope state) {
    tick(state.concurrentAccurateRateMeter_atomicLongTicksCounter);
  }

  @Benchmark
  @Group("parallel$2_tick_concurrentAccurateRateMeter_atomicLongTicksCounter")
  @GroupThreads(2)
  public void parallel$2_tick_concurrentAccurateRateMeter_atomicLongTicksCounter(final RateMeterContainer_GroupScope state) {
    tick(state.concurrentAccurateRateMeter_atomicLongTicksCounter);
  }

  @Benchmark
  @Group("parallel$4_tick_concurrentAccurateRateMeter_atomicLongTicksCounter")
  @GroupThreads(4)
  public void parallel$4_tick_concurrentAccurateRateMeter_atomicLongTicksCounter(final RateMeterContainer_GroupScope state) {
    tick(state.concurrentAccurateRateMeter_atomicLongTicksCounter);
    state.all.add(state.concurrentAccurateRateMeter_atomicLongTicksCounter.rate(nanoTime()));
//    if (state.concurrentAccurateRateMeter_atomicLongTicksCounter.ticksTotalCount() % 50_000 == 0) {
//      System.out.println(state.concurrentAccurateRateMeter_atomicLongTicksCounter.rate(nanoTime(), Duration.ofMillis(1)));
//    }//TODO
  }

  @Benchmark
  @Group("parallel$1_tick_concurrentAccurateRateMeter_longAdderTicksCounter")
  @GroupThreads(1)
  public void parallel$1_tick_concurrentAccurateRateMeter_longAdderTicksCounter(final RateMeterContainer_GroupScope state) {
    tick(state.concurrentAccurateRateMeter_longAdderTicksCounter);
  }

  @Benchmark
  @Group("parallel$2_tick_concurrentAccurateRateMeter_longAdderTicksCounter")
  @GroupThreads(2)
  public void parallel$2_tick_concurrentAccurateRateMeter_longAdderTicksCounter(final RateMeterContainer_GroupScope state) {
    tick(state.concurrentAccurateRateMeter_longAdderTicksCounter);
  }

  @Benchmark
  @Group("parallel$4_tick_concurrentAccurateRateMeter_longAdderTicksCounter")
  @GroupThreads(4)
  public void parallel$4_tick_concurrentAccurateRateMeter_longAdderTicksCounter(final RateMeterContainer_GroupScope state) {
    tick(state.concurrentAccurateRateMeter_longAdderTicksCounter);
  }

  @Benchmark
  @Group("parallel$1_tick$100rate$1_concurrentAccurateRateMeter_atomicLongTicksCounter")
  @GroupThreads(1)
  public void parallel$1_tick$100rate$1_concurrentAccurateRateMeter_atomicLongTicksCounter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentAccurateRateMeter_atomicLongTicksCounter, bh, counter.v++, 100);
  }

  @Benchmark
  @Group("parallel$2_tick$100rate$1_concurrentAccurateRateMeter_atomicLongTicksCounter")
  @GroupThreads(2)
  public void parallel$2_tick$100rate$1_concurrentAccurateRateMeter_atomicLongTicksCounter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentAccurateRateMeter_atomicLongTicksCounter, bh, counter.v++, 100);
  }

  @Benchmark
  @Group("parallel$4_tick$100rate$1_concurrentAccurateRateMeter_atomicLongTicksCounter")
  @GroupThreads(4)
  public void parallel$4_tick$100rate$1_concurrentAccurateRateMeter_atomicLongTicksCounter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentAccurateRateMeter_atomicLongTicksCounter, bh, counter.v++, 100);
  }

  @Benchmark
  @Group("parallel$1_tick$100rate$1_concurrentAccurateRateMeter_longAdderTicksCounter")
  @GroupThreads(1)
  public void parallel$1_tick$100rate$1_concurrentAccurateRateMeter_longAdderTicksCounter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentAccurateRateMeter_longAdderTicksCounter, bh, counter.v++, 100);
  }

  @Benchmark
  @Group("parallel$2_tick$100rate$1_concurrentAccurateRateMeter_longAdderTicksCounter")
  @GroupThreads(2)
  public void parallel$2_tick$100rate$1_concurrentAccurateRateMeter_longAdderTicksCounter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentAccurateRateMeter_longAdderTicksCounter, bh, counter.v++, 100);
  }

  @Benchmark
  @Group("parallel$4_tick$100rate$1_concurrentAccurateRateMeter_longAdderTicksCounter")
  @GroupThreads(4)
  public void parallel$4_tick$100rate$1_concurrentAccurateRateMeter_longAdderTicksCounter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentAccurateRateMeter_longAdderTicksCounter, bh, counter.v++, 100);
  }

  @Benchmark
  @Group("parallel$1_tick$10rate$1_concurrentAccurateRateMeter_atomicLongTicksCounter")
  @GroupThreads(1)
  public void parallel$1_tick$10rate$1_concurrentAccurateRateMeter_atomicLongTicksCounter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentAccurateRateMeter_atomicLongTicksCounter, bh, counter.v++, 10);
  }

  @Benchmark
  @Group("parallel$2_tick$10rate$1_concurrentAccurateRateMeter_atomicLongTicksCounter")
  @GroupThreads(2)
  public void parallel$2_tick$10rate$1_concurrentAccurateRateMeter_atomicLongTicksCounter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentAccurateRateMeter_atomicLongTicksCounter, bh, counter.v++, 10);
  }

  @Benchmark
  @Group("parallel$4_tick$10rate$1_concurrentAccurateRateMeter_atomicLongTicksCounter")
  @GroupThreads(4)
  public void parallel$4_tick$10rate$1_concurrentAccurateRateMeter_atomicLongTicksCounter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentAccurateRateMeter_atomicLongTicksCounter, bh, counter.v++, 10);
  }

  @Benchmark
  @Group("parallel$1_tick$10rate$1_concurrentAccurateRateMeter_longAdderTicksCounter")
  @GroupThreads(1)
  public void parallel$1_tick$10rate$1_concurrentAccurateRateMeter_longAdderTicksCounter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentAccurateRateMeter_longAdderTicksCounter, bh, counter.v++, 10);
  }

  @Benchmark
  @Group("parallel$2_tick$10rate$1_concurrentAccurateRateMeter_longAdderTicksCounter")
  @GroupThreads(2)
  public void parallel$2_tick$10rate$1_concurrentAccurateRateMeter_longAdderTicksCounter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentAccurateRateMeter_longAdderTicksCounter, bh, counter.v++, 10);
  }

  @Benchmark
  @Group("parallel$4_tick$10rate$1_concurrentAccurateRateMeter_longAdderTicksCounter")
  @GroupThreads(4)
  public void parallel$4_tick$10rate$1_concurrentAccurateRateMeter_longAdderTicksCounter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentAccurateRateMeter_longAdderTicksCounter, bh, counter.v++, 10);
  }

  @Benchmark
  @Group("parallel$4_tick$2_rate$2_concurrentAccurateRateMeter_atomicLongTicksCounter")
  @GroupThreads(2)
  public void parallel$4_TICK$2_rate$2_concurrentAccurateRateMeter_atomicLongTicksCounter(final RateMeterContainer_GroupScope state) {
    tick(state.concurrentAccurateRateMeter_atomicLongTicksCounter);
  }

  @Benchmark
  @Group("parallel$4_tick$2_rate$2_concurrentAccurateRateMeter_atomicLongTicksCounter")
  @GroupThreads(2)
  public void parallel$4_tick$2_RATE$2_concurrentAccurateRateMeter_atomicLongTicksCounter(final RateMeterContainer_GroupScope state, final Blackhole bh) {
    rate(state.concurrentAccurateRateMeter_atomicLongTicksCounter, bh);
  }

  @Benchmark
  @Group("parallel$4_tick$3_rate$1_concurrentAccurateRateMeter_atomicLongTicksCounter")
  @GroupThreads(3)
  public void parallel$4_TICK$3_rate$1_concurrentAccurateRateMeter_atomicLongTicksCounter(final RateMeterContainer_GroupScope state) {
    tick(state.concurrentAccurateRateMeter_atomicLongTicksCounter);
  }

  @Benchmark
  @Group("parallel$4_tick$3_rate$1_concurrentAccurateRateMeter_atomicLongTicksCounter")
  @GroupThreads(1)
  public void parallel$4_tick$3_RATE$1_concurrentAccurateRateMeter_atomicLongTicksCounter(final RateMeterContainer_GroupScope state, final Blackhole bh) {
    rate(state.concurrentAccurateRateMeter_atomicLongTicksCounter, bh);
  }

  @Benchmark
  @Group("parallel$4_tick$2_rate$2_concurrentAccurateRateMeter_longAdderTicksCounter")
  @GroupThreads(2)
  public void parallel$4_TICK$2_rate$2_concurrentAccurateRateMeter_longAdderTicksCounter(final RateMeterContainer_GroupScope state) {
    tick(state.concurrentAccurateRateMeter_longAdderTicksCounter);
  }

  @Benchmark
  @Group("parallel$4_tick$2_rate$2_concurrentAccurateRateMeter_longAdderTicksCounter")
  @GroupThreads(2)
  public void parallel$4_tick$2_RATE$2_concurrentAccurateRateMeter_longAdderTicksCounter(final RateMeterContainer_GroupScope state, final Blackhole bh) {
    rate(state.concurrentAccurateRateMeter_longAdderTicksCounter, bh);
  }

  @Benchmark
  @Group("parallel$4_tick$3_rate$1_concurrentAccurateRateMeter_longAdderTicksCounter")
  @GroupThreads(3)
  public void parallel$4_TICK$3_rate$1_concurrentAccurateRateMeter_longAdderTicksCounter(final RateMeterContainer_GroupScope state) {
    tick(state.concurrentAccurateRateMeter_longAdderTicksCounter);
  }

  @Benchmark
  @Group("parallel$4_tick$3_rate$1_concurrentAccurateRateMeter_longAdderTicksCounter")
  @GroupThreads(1)
  public void parallel$4_tick$3_RATE$1_concurrentAccurateRateMeter_longAdderTicksCounter(final RateMeterContainer_GroupScope state, final Blackhole bh) {
    rate(state.concurrentAccurateRateMeter_longAdderTicksCounter, bh);
  }

  private static final void tick(final RateMeter rm) {
    rm.tick(1, nanoTime());
  }

  private static final void rate(final RateMeter rm, final Blackhole bh) {
    bh.consume(rm.rate());
  }

  private static final void tickAndRate(final RateMeter rm, final Blackhole bh, final int counter, final int tickToRateRatio) {
    rm.tick(1, nanoTime());
    if (counter % tickToRateRatio == 0) {
      bh.consume(rm.rate());
    }
  }

  private static final long nanoTime() {
    return millisInsteadOfNanos
        ? TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis())//this timer is not monotonic
        : System.nanoTime();
  }

  @State(Scope.Thread)
  public static class RateMeterContainer_ThreadScope {
    Collection<Double> all;
    AccurateRateMeter accurateRateMeter_longTicksCounter;
    AccurateRateMeter accurateRateMeter_atomicLongTicksCounter;
    AccurateRateMeter accurateRateMeter_longAdderTicksCounter;
    ConcurrentAccurateRateMeter concurrentAccurateRateMeter_longTicksCounter;
    ConcurrentAccurateRateMeter concurrentAccurateRateMeter_atomicLongTicksCounter;
    ConcurrentAccurateRateMeter concurrentAccurateRateMeter_longAdderTicksCounter;

    public RateMeterContainer_ThreadScope() {
    }

    @Setup(Level.Iteration)
    public final void setup() {
      all = new ConcurrentLinkedQueue<>();//TODO
      accurateRateMeter_longTicksCounter = new AccurateRateMeter(nanoTime(), samplesInterval,
          rateMeterConfigBuilderSuppplier.get()
              .setTicksCounterSupplier(LongTicksCounter::new)
              .build());
      accurateRateMeter_atomicLongTicksCounter = new AccurateRateMeter(nanoTime(), samplesInterval,
          rateMeterConfigBuilderSuppplier.get()
              .setTicksCounterSupplier(AtomicLongTicksCounter::new)
              .build());
      accurateRateMeter_longAdderTicksCounter = new AccurateRateMeter(nanoTime(), samplesInterval,
          rateMeterConfigBuilderSuppplier.get()
              .setTicksCounterSupplier(LongAdderTicksCounter::new)
              .build());
      concurrentAccurateRateMeter_longTicksCounter = new ConcurrentAccurateRateMeter(nanoTime(), samplesInterval,
          rateMeterConfigBuilderSuppplier.get()
              .setTicksCounterSupplier(LongTicksCounter::new)
              .build());
      concurrentAccurateRateMeter_atomicLongTicksCounter = new ConcurrentAccurateRateMeter(nanoTime(), samplesInterval,
          rateMeterConfigBuilderSuppplier.get()
              .setTicksCounterSupplier(AtomicLongTicksCounter::new)
              .build());
      concurrentAccurateRateMeter_longAdderTicksCounter = new ConcurrentAccurateRateMeter(nanoTime(), samplesInterval,
          rateMeterConfigBuilderSuppplier.get()
              .setTicksCounterSupplier(LongAdderTicksCounter::new)
              .build());
    }

    @TearDown(Level.Iteration)
    public final void tearDown() {//TODO
      final double avg = all.isEmpty() ? Double.NaN : all.stream().mapToDouble(Double::doubleValue).sum() / all.size();
      System.out.println("AVG " + avg);
      all.clear();
    }
  }

  @State(Scope.Group)
  public static class RateMeterContainer_GroupScope {
    Collection<Double> all;
    ConcurrentAccurateRateMeter concurrentAccurateRateMeter_atomicLongTicksCounter;
    ConcurrentAccurateRateMeter concurrentAccurateRateMeter_longAdderTicksCounter;

    public RateMeterContainer_GroupScope() {
    }

    @Setup(Level.Iteration)
    public final void setup() {
      all = new ConcurrentLinkedQueue<>();//TODO
      concurrentAccurateRateMeter_atomicLongTicksCounter = new ConcurrentAccurateRateMeter(nanoTime(), samplesInterval,
          rateMeterConfigBuilderSuppplier.get()
              .setTicksCounterSupplier(AtomicLongTicksCounter::new)
              .build());
      concurrentAccurateRateMeter_longAdderTicksCounter = new ConcurrentAccurateRateMeter(nanoTime(), samplesInterval,
          rateMeterConfigBuilderSuppplier.get()
              .setTicksCounterSupplier(LongAdderTicksCounter::new)
              .build());
    }

    @TearDown(Level.Iteration)
    public final void tearDown() {//TODO
      final double avg = all.isEmpty() ? Double.NaN : all.stream().mapToDouble(Double::doubleValue).sum() / all.size();
      System.out.println("AVG " + avg);
      all.clear();
    }
  }

  @State(Scope.Thread)
  public static class IntCounter_ThreadScope {
    int v;

    public IntCounter_ThreadScope() {
    }

    @Setup(Level.Iteration)
    public final void setup() {
      v = 0;
    }
  }
}