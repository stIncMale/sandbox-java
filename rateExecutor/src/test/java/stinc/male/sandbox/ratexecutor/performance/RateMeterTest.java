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
import stinc.male.sandbox.ratexecutor.TreeMapRateMeter;
import stinc.male.sandbox.ratexecutor.AtomicLongTicksCounter;
import stinc.male.sandbox.ratexecutor.ConcurrentSkipListMapRateMeter;
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
  public void serial_tick_treeMapRateMeter_longTicksCounter(final RateMeterContainer_ThreadScope state) {
    tick(state.treeMapRateMeter_longTicksCounter);
  }

  @Benchmark
  public void serial_tick_treeMapRateMeter_atomicLongTicksCounter(final RateMeterContainer_ThreadScope state) {
    tick(state.treeMapRateMeter_atomicLongTicksCounter);
    state.all.add(state.treeMapRateMeter_atomicLongTicksCounter.rate(nanoTime()));
//    if (state.treeMapRateMeter_atomicLongTicksCounter.ticksTotalCount() % 50_000 == 0) {
//      System.out.println(state.treeMapRateMeter_atomicLongTicksCounter.rate(nanoTime(), Duration.ofMillis(1)));
//    }//TODO
  }

  @Benchmark
  public void serial_tick_treeMapRateMeter_longAdderTicksCounter(final RateMeterContainer_ThreadScope state) {
    tick(state.treeMapRateMeter_longAdderTicksCounter);
  }

  @Benchmark
  public void serial_tick_concurrentSkipListMapRateMeter_longTicksCounter(final RateMeterContainer_ThreadScope state) {
    tick(state.concurrentSkipListMapRateMeter_longTicksCounter);
  }

  @Benchmark
  public void serial_tick_concurrentSkipListMapRateMeter_atomicLongTicksCounter(final RateMeterContainer_ThreadScope state) {
    tick(state.concurrentSkipListMapRateMeter_atomicLongTicksCounter);
  }

  @Benchmark
  public void serial_tick_concurrentSkipListMapRateMeter_longAdderTicksCounter(final RateMeterContainer_ThreadScope state) {
    tick(state.concurrentSkipListMapRateMeter_longAdderTicksCounter);
  }

  @Benchmark
  public void serial_tick$100rate$1_treeMapRateMeter_longTicksCounter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.treeMapRateMeter_longTicksCounter, bh, counter.v++, 100);
  }

  @Benchmark
  public void serial_tick$100rate$1_treeMapRateMeter_atomicLongTicksCounter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.treeMapRateMeter_atomicLongTicksCounter, bh, counter.v++, 100);
  }

  @Benchmark
  public void serial_tick$100rate$1_treeMapRateMeter_longAdderTicksCounter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.treeMapRateMeter_longAdderTicksCounter, bh, counter.v++, 100);
  }

  @Benchmark
  public void serial_tick$100rate$1_concurrentSkipListMapRateMeter_longTicksCounter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentSkipListMapRateMeter_longTicksCounter, bh, counter.v++, 100);
  }

  @Benchmark
  public void serial_tick$100rate$1_concurrentSkipListMapRateMeter_atomicLongTicksCounter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentSkipListMapRateMeter_atomicLongTicksCounter, bh, counter.v++, 100);
  }

  @Benchmark
  public void serial_tick$100rate$1_concurrentSkipListMapRateMeter_longAdderTicksCounter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentSkipListMapRateMeter_longAdderTicksCounter, bh, counter.v++, 100);
  }

  @Benchmark
  public void serial_tick$10rate$1_treeMapRateMeter_longTicksCounter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.treeMapRateMeter_longTicksCounter, bh, counter.v++, 10);
  }

  @Benchmark
  public void serial_tick$10rate$1_treeMapRateMeter_atomicLongTicksCounter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.treeMapRateMeter_atomicLongTicksCounter, bh, counter.v++, 10);
  }

  @Benchmark
  public void serial_tick$10rate$1_treeMapRateMeter_longAdderTicksCounter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.treeMapRateMeter_longAdderTicksCounter, bh, counter.v++, 10);
  }

  @Benchmark
  public void serial_tick$10rate$1_concurrentSkipListMapRateMeter_longTicksCounter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentSkipListMapRateMeter_longTicksCounter, bh, counter.v++, 10);
  }

  @Benchmark
  public void serial_tick$10rate$1_concurrentSkipListMapRateMeter_atomicLongTicksCounter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentSkipListMapRateMeter_atomicLongTicksCounter, bh, counter.v++, 10);
  }

  @Benchmark
  public void serial_tick$10rate$1_concurrentSkipListMapRateMeter_longAdderTicksCounter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentSkipListMapRateMeter_longAdderTicksCounter, bh, counter.v++, 10);
  }

  @Benchmark
  @Group("parallel_tick_concurrentSkipListMapRateMeter_atomicLongTicksCounter")
  @GroupThreads(1)
  public void parallel$1_tick_concurrentSkipListMapRateMeter_atomicLongTicksCounter(final RateMeterContainer_GroupScope state) {
    tick(state.concurrentSkipListMapRateMeter_atomicLongTicksCounter);
  }

  @Benchmark
  @Group("parallel$2_tick_concurrentSkipListMapRateMeter_atomicLongTicksCounter")
  @GroupThreads(2)
  public void parallel$2_tick_concurrentSkipListMapRateMeter_atomicLongTicksCounter(final RateMeterContainer_GroupScope state) {
    tick(state.concurrentSkipListMapRateMeter_atomicLongTicksCounter);
  }

  @Benchmark
  @Group("parallel$4_tick_concurrentSkipListMapRateMeter_atomicLongTicksCounter")
  @GroupThreads(4)
  public void parallel$4_tick_concurrentSkipListMapRateMeter_atomicLongTicksCounter(final RateMeterContainer_GroupScope state) {
    tick(state.concurrentSkipListMapRateMeter_atomicLongTicksCounter);
    state.all.add(state.concurrentSkipListMapRateMeter_atomicLongTicksCounter.rate(nanoTime()));
//    if (state.concurrentSkipListMapRateMeter_atomicLongTicksCounter.ticksTotalCount() % 50_000 == 0) {
//      System.out.println(state.concurrentSkipListMapRateMeter_atomicLongTicksCounter.rate(nanoTime(), Duration.ofMillis(1)));
//    }//TODO
  }

  @Benchmark
  @Group("parallel$1_tick_concurrentSkipListMapRateMeter_longAdderTicksCounter")
  @GroupThreads(1)
  public void parallel$1_tick_concurrentSkipListMapRateMeter_longAdderTicksCounter(final RateMeterContainer_GroupScope state) {
    tick(state.concurrentSkipListMapRateMeter_longAdderTicksCounter);
  }

  @Benchmark
  @Group("parallel$2_tick_concurrentSkipListMapRateMeter_longAdderTicksCounter")
  @GroupThreads(2)
  public void parallel$2_tick_concurrentSkipListMapRateMeter_longAdderTicksCounter(final RateMeterContainer_GroupScope state) {
    tick(state.concurrentSkipListMapRateMeter_longAdderTicksCounter);
  }

  @Benchmark
  @Group("parallel$4_tick_concurrentSkipListMapRateMeter_longAdderTicksCounter")
  @GroupThreads(4)
  public void parallel$4_tick_concurrentSkipListMapRateMeter_longAdderTicksCounter(final RateMeterContainer_GroupScope state) {
    tick(state.concurrentSkipListMapRateMeter_longAdderTicksCounter);
  }

  @Benchmark
  @Group("parallel$1_tick$100rate$1_concurrentSkipListMapRateMeter_atomicLongTicksCounter")
  @GroupThreads(1)
  public void parallel$1_tick$100rate$1_concurrentSkipListMapRateMeter_atomicLongTicksCounter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentSkipListMapRateMeter_atomicLongTicksCounter, bh, counter.v++, 100);
  }

  @Benchmark
  @Group("parallel$2_tick$100rate$1_concurrentSkipListMapRateMeter_atomicLongTicksCounter")
  @GroupThreads(2)
  public void parallel$2_tick$100rate$1_concurrentSkipListMapRateMeter_atomicLongTicksCounter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentSkipListMapRateMeter_atomicLongTicksCounter, bh, counter.v++, 100);
  }

  @Benchmark
  @Group("parallel$4_tick$100rate$1_concurrentSkipListMapRateMeter_atomicLongTicksCounter")
  @GroupThreads(4)
  public void parallel$4_tick$100rate$1_concurrentSkipListMapRateMeter_atomicLongTicksCounter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentSkipListMapRateMeter_atomicLongTicksCounter, bh, counter.v++, 100);
  }

  @Benchmark
  @Group("parallel$1_tick$100rate$1_concurrentSkipListMapRateMeter_longAdderTicksCounter")
  @GroupThreads(1)
  public void parallel$1_tick$100rate$1_concurrentSkipListMapRateMeter_longAdderTicksCounter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentSkipListMapRateMeter_longAdderTicksCounter, bh, counter.v++, 100);
  }

  @Benchmark
  @Group("parallel$2_tick$100rate$1_concurrentSkipListMapRateMeter_longAdderTicksCounter")
  @GroupThreads(2)
  public void parallel$2_tick$100rate$1_concurrentSkipListMapRateMeter_longAdderTicksCounter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentSkipListMapRateMeter_longAdderTicksCounter, bh, counter.v++, 100);
  }

  @Benchmark
  @Group("parallel$4_tick$100rate$1_concurrentSkipListMapRateMeter_longAdderTicksCounter")
  @GroupThreads(4)
  public void parallel$4_tick$100rate$1_concurrentSkipListMapRateMeter_longAdderTicksCounter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentSkipListMapRateMeter_longAdderTicksCounter, bh, counter.v++, 100);
  }

  @Benchmark
  @Group("parallel$1_tick$10rate$1_concurrentSkipListMapRateMeter_atomicLongTicksCounter")
  @GroupThreads(1)
  public void parallel$1_tick$10rate$1_concurrentSkipListMapRateMeter_atomicLongTicksCounter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentSkipListMapRateMeter_atomicLongTicksCounter, bh, counter.v++, 10);
  }

  @Benchmark
  @Group("parallel$2_tick$10rate$1_concurrentSkipListMapRateMeter_atomicLongTicksCounter")
  @GroupThreads(2)
  public void parallel$2_tick$10rate$1_concurrentSkipListMapRateMeter_atomicLongTicksCounter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentSkipListMapRateMeter_atomicLongTicksCounter, bh, counter.v++, 10);
  }

  @Benchmark
  @Group("parallel$4_tick$10rate$1_concurrentSkipListMapRateMeter_atomicLongTicksCounter")
  @GroupThreads(4)
  public void parallel$4_tick$10rate$1_concurrentSkipListMapRateMeter_atomicLongTicksCounter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentSkipListMapRateMeter_atomicLongTicksCounter, bh, counter.v++, 10);
  }

  @Benchmark
  @Group("parallel$1_tick$10rate$1_concurrentSkipListMapRateMeter_longAdderTicksCounter")
  @GroupThreads(1)
  public void parallel$1_tick$10rate$1_concurrentSkipListMapRateMeter_longAdderTicksCounter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentSkipListMapRateMeter_longAdderTicksCounter, bh, counter.v++, 10);
  }

  @Benchmark
  @Group("parallel$2_tick$10rate$1_concurrentSkipListMapRateMeter_longAdderTicksCounter")
  @GroupThreads(2)
  public void parallel$2_tick$10rate$1_concurrentSkipListMapRateMeter_longAdderTicksCounter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentSkipListMapRateMeter_longAdderTicksCounter, bh, counter.v++, 10);
  }

  @Benchmark
  @Group("parallel$4_tick$10rate$1_concurrentSkipListMapRateMeter_longAdderTicksCounter")
  @GroupThreads(4)
  public void parallel$4_tick$10rate$1_concurrentSkipListMapRateMeter_longAdderTicksCounter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentSkipListMapRateMeter_longAdderTicksCounter, bh, counter.v++, 10);
  }

  @Benchmark
  @Group("parallel$4_tick$2_rate$2_concurrentSkipListMapRateMeter_atomicLongTicksCounter")
  @GroupThreads(2)
  public void parallel$4_TICK$2_rate$2_concurrentSkipListMapRateMeter_atomicLongTicksCounter(final RateMeterContainer_GroupScope state) {
    tick(state.concurrentSkipListMapRateMeter_atomicLongTicksCounter);
  }

  @Benchmark
  @Group("parallel$4_tick$2_rate$2_concurrentSkipListMapRateMeter_atomicLongTicksCounter")
  @GroupThreads(2)
  public void parallel$4_tick$2_RATE$2_concurrentSkipListMapRateMeter_atomicLongTicksCounter(final RateMeterContainer_GroupScope state, final Blackhole bh) {
    rate(state.concurrentSkipListMapRateMeter_atomicLongTicksCounter, bh);
  }

  @Benchmark
  @Group("parallel$4_tick$3_rate$1_concurrentSkipListMapRateMeter_atomicLongTicksCounter")
  @GroupThreads(3)
  public void parallel$4_TICK$3_rate$1_concurrentSkipListMapRateMeter_atomicLongTicksCounter(final RateMeterContainer_GroupScope state) {
    tick(state.concurrentSkipListMapRateMeter_atomicLongTicksCounter);
  }

  @Benchmark
  @Group("parallel$4_tick$3_rate$1_concurrentSkipListMapRateMeter_atomicLongTicksCounter")
  @GroupThreads(1)
  public void parallel$4_tick$3_RATE$1_concurrentSkipListMapRateMeter_atomicLongTicksCounter(final RateMeterContainer_GroupScope state, final Blackhole bh) {
    rate(state.concurrentSkipListMapRateMeter_atomicLongTicksCounter, bh);
  }

  @Benchmark
  @Group("parallel$4_tick$2_rate$2_concurrentSkipListMapRateMeter_longAdderTicksCounter")
  @GroupThreads(2)
  public void parallel$4_TICK$2_rate$2_concurrentSkipListMapRateMeter_longAdderTicksCounter(final RateMeterContainer_GroupScope state) {
    tick(state.concurrentSkipListMapRateMeter_longAdderTicksCounter);
  }

  @Benchmark
  @Group("parallel$4_tick$2_rate$2_concurrentSkipListMapRateMeter_longAdderTicksCounter")
  @GroupThreads(2)
  public void parallel$4_tick$2_RATE$2_concurrentSkipListMapRateMeter_longAdderTicksCounter(final RateMeterContainer_GroupScope state, final Blackhole bh) {
    rate(state.concurrentSkipListMapRateMeter_longAdderTicksCounter, bh);
  }

  @Benchmark
  @Group("parallel$4_tick$3_rate$1_concurrentSkipListMapRateMeter_longAdderTicksCounter")
  @GroupThreads(3)
  public void parallel$4_TICK$3_rate$1_concurrentSkipListMapRateMeter_longAdderTicksCounter(final RateMeterContainer_GroupScope state) {
    tick(state.concurrentSkipListMapRateMeter_longAdderTicksCounter);
  }

  @Benchmark
  @Group("parallel$4_tick$3_rate$1_concurrentSkipListMapRateMeter_longAdderTicksCounter")
  @GroupThreads(1)
  public void parallel$4_tick$3_RATE$1_concurrentSkipListMapRateMeter_longAdderTicksCounter(final RateMeterContainer_GroupScope state, final Blackhole bh) {
    rate(state.concurrentSkipListMapRateMeter_longAdderTicksCounter, bh);
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
    TreeMapRateMeter treeMapRateMeter_longTicksCounter;
    TreeMapRateMeter treeMapRateMeter_atomicLongTicksCounter;
    TreeMapRateMeter treeMapRateMeter_longAdderTicksCounter;
    ConcurrentSkipListMapRateMeter concurrentSkipListMapRateMeter_longTicksCounter;
    ConcurrentSkipListMapRateMeter concurrentSkipListMapRateMeter_atomicLongTicksCounter;
    ConcurrentSkipListMapRateMeter concurrentSkipListMapRateMeter_longAdderTicksCounter;

    public RateMeterContainer_ThreadScope() {
    }

    @Setup(Level.Iteration)
    public final void setup() {
      all = new ConcurrentLinkedQueue<>();//TODO
      treeMapRateMeter_longTicksCounter = new TreeMapRateMeter(nanoTime(), samplesInterval,
          rateMeterConfigBuilderSuppplier.get()
              .setTicksCounterSupplier(LongTicksCounter::new)
              .build());
      treeMapRateMeter_atomicLongTicksCounter = new TreeMapRateMeter(nanoTime(), samplesInterval,
          rateMeterConfigBuilderSuppplier.get()
              .setTicksCounterSupplier(AtomicLongTicksCounter::new)
              .build());
      treeMapRateMeter_longAdderTicksCounter = new TreeMapRateMeter(nanoTime(), samplesInterval,
          rateMeterConfigBuilderSuppplier.get()
              .setTicksCounterSupplier(LongAdderTicksCounter::new)
              .build());
      concurrentSkipListMapRateMeter_longTicksCounter = new ConcurrentSkipListMapRateMeter(nanoTime(), samplesInterval,
          rateMeterConfigBuilderSuppplier.get()
              .setTicksCounterSupplier(LongTicksCounter::new)
              .build());
      concurrentSkipListMapRateMeter_atomicLongTicksCounter = new ConcurrentSkipListMapRateMeter(nanoTime(), samplesInterval,
          rateMeterConfigBuilderSuppplier.get()
              .setTicksCounterSupplier(AtomicLongTicksCounter::new)
              .build());
      concurrentSkipListMapRateMeter_longAdderTicksCounter = new ConcurrentSkipListMapRateMeter(nanoTime(), samplesInterval,
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
    ConcurrentSkipListMapRateMeter concurrentSkipListMapRateMeter_atomicLongTicksCounter;
    ConcurrentSkipListMapRateMeter concurrentSkipListMapRateMeter_longAdderTicksCounter;

    public RateMeterContainer_GroupScope() {
    }

    @Setup(Level.Iteration)
    public final void setup() {
      all = new ConcurrentLinkedQueue<>();//TODO
      concurrentSkipListMapRateMeter_atomicLongTicksCounter = new ConcurrentSkipListMapRateMeter(nanoTime(), samplesInterval,
          rateMeterConfigBuilderSuppplier.get()
              .setTicksCounterSupplier(AtomicLongTicksCounter::new)
              .build());
      concurrentSkipListMapRateMeter_longAdderTicksCounter = new ConcurrentSkipListMapRateMeter(nanoTime(), samplesInterval,
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