package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
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
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import stinc.male.PerformanceTest;
import stinc.male.sandbox.ratexecutor.RateMeterConfig.Builder;
import static org.openjdk.jmh.runner.options.TimeValue.milliseconds;

@Category(PerformanceTest.class)
public class RateMeterPerformanceTest {
  private static final Duration samplesInterval = Duration.ofMillis(100);
  private static final Duration timeSensitivity = Duration.ofMillis(10);
  private static final boolean server = true;
  private static final boolean quick = false;
  private static final Supplier<ChainedOptionsBuilder> jmhOptionsBuilderSupplier = () -> {
    final ChainedOptionsBuilder result = new OptionsBuilder().mode(Mode.Throughput)
        .jvmArgsPrepend(server ? "-server" : "-client")
        .timeUnit(TimeUnit.MILLISECONDS)
        .syncIterations(true)
        .shouldFailOnError(true)
        .shouldDoGC(true)
        .timeout(milliseconds(30_000));
    if (quick) {
      result.warmupTime(milliseconds(samplesInterval.toMillis()))
          .warmupIterations(1)
          .measurementTime(milliseconds(1_000))
          .measurementIterations(1)
          .forks(1);
    } else {
      result.warmupTime(milliseconds(1_000))
          .warmupIterations(3)
          .measurementTime(milliseconds(1_500))
          .measurementIterations(3)
          .forks(3);
    }
    return result;
  };
  private static final Supplier<Builder> rateMeterConfigBuilderSuppplier = () -> RateMeterConfig.newBuilder();

  public RateMeterPerformanceTest() {
  }

  @Test
  public void serialBaseline() throws RunnerException {
    new Runner(jmhOptionsBuilderSupplier.get()
        .include(RateMeterPerformanceTest.class.getName() + ".baseline_.*")
        .threads(1)
        .build())
        .run();
  }

  @Test
  public void serialTreeMapRateMeter() throws RunnerException {
    new Runner(jmhOptionsBuilderSupplier.get()
        .include(getClass().getName() + ".serial_.*treeMapRateMeter")
        .threads(1)
        .build())
        .run();
  }

  @Test
  public void serialConcurrentSkipListMapRateMeter() throws RunnerException {
    new Runner(jmhOptionsBuilderSupplier.get()
            .include(getClass().getName() + ".serial_.*concurrentSkipListMapRateMeter")
            .threads(1)
            .build())
            .run();
  }

  @Test
  public void serialArrayRateMeter() throws RunnerException {
    new Runner(jmhOptionsBuilderSupplier.get()
            .include(getClass().getName() + ".serial_.*arrayRateMeter")
            .threads(1)
            .build())
            .run();
  }

  @Test
  public void parallelBaseline() throws RunnerException {
    new Runner(jmhOptionsBuilderSupplier.get()
        .include(RateMeterPerformanceTest.class.getName() + ".baseline_.*")
        .threads(4)
        .build())
        .run();
  }

  @Test
  public void parallelTest() throws RunnerException {
    new Runner(jmhOptionsBuilderSupplier.get()
        .include(getClass().getName() + ".parallel_.*concurrentSkipListMapRateMeter")
        .threads(4)
        .build())
        .run();
  }

  @Benchmark
  public void baseline_empty() {
  }

  @Benchmark
  public void baseline_time(final Blackhole bh) {
    bh.consume(nanoTime());
  }

  @Benchmark
  public void intCounter_baseline(IntCounter_ThreadScope counter, final Blackhole bh) {
    counter.v++;
    bh.consume(counter.v % 10);
  }

  @Benchmark
  public void serial_tick_treeMapRateMeter(final RateMeterContainer_ThreadScope state) {
    tick(state.treeMapRateMeter);
  }

  @Benchmark
  public void serial_tick_concurrentSkipListMapRateMeter(final RateMeterContainer_ThreadScope state) {
    tick(state.concurrentSkipListMapRateMeter);
  }

  @Benchmark
  public void serial_tick_arrayRateMeter(final RateMeterContainer_ThreadScope state) {
    tick(state.arrayRateMeter);
  }

  @Benchmark
  public void serial_tick$10rate$1_treeMapRateMeter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.treeMapRateMeter, bh, counter.v++, 10);
  }

  @Benchmark
  public void serial_tick$10rate$1_concurrentSkipListMapRateMeter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentSkipListMapRateMeter, bh, counter.v++, 10);
  }

  @Benchmark
  public void serial_tick$10rate$1_arrayRateMeter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.arrayRateMeter, bh, counter.v++, 10);
  }

  @Benchmark
  public void serial_tick$1rate$1_treeMapRateMeter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.treeMapRateMeter, bh, counter.v++, 1);
  }

  @Benchmark
  public void serial_tick$1rate$1_concurrentSkipListMapRateMeter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentSkipListMapRateMeter, bh, counter.v++, 1);
  }

  @Benchmark
  public void serial_tick$1rate$1_arrayRateMeter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.arrayRateMeter, bh, counter.v++, 1);
  }

  @Benchmark
  public void serial_tick$1rate$10_treeMapRateMeter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    rateAndTick(state.treeMapRateMeter, bh, counter.v++, 10);
  }

  @Benchmark
  public void serial_tick$1rate$10_concurrentSkipListMapRateMeter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    rateAndTick(state.concurrentSkipListMapRateMeter, bh, counter.v++, 10);
  }

  @Benchmark
  public void serial_tick$1rate$10_arrayRateMeter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    rateAndTick(state.arrayRateMeter, bh, counter.v++, 10);
  }

  @Benchmark
  @Group("parallel_1_tick_concurrentSkipListMapRateMeter")
  @GroupThreads(1)
  public void parallel_1_tick_concurrentSkipListMapRateMeter(final RateMeterContainer_GroupScope state) {
    tick(state.concurrentSkipListMapRateMeter);
  }

  @Benchmark
  @Group("parallel_2_tick_concurrentSkipListMapRateMeter")
  @GroupThreads(2)
  public void parallel_2_tick_concurrentSkipListMapRateMeter(final RateMeterContainer_GroupScope state) {
    tick(state.concurrentSkipListMapRateMeter);
  }

  @Benchmark
  @Group("parallel_4_tick_concurrentSkipListMapRateMeter")
  @GroupThreads(4)
  public void parallel_4_tick_concurrentSkipListMapRateMeter(final RateMeterContainer_GroupScope state) {
    tick(state.concurrentSkipListMapRateMeter);
  }

  @Benchmark
  @Group("parallel_1_tick$10rate$1_concurrentSkipListMapRateMeter")
  @GroupThreads(1)
  public void parallel_1_tick$10rate$1_concurrentSkipListMapRateMeter_longAdderTicksCounter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentSkipListMapRateMeter, bh, counter.v++, 10);
  }

  @Benchmark
  @Group("parallel_2_tick$10rate$1_concurrentSkipListMapRateMeter")
  @GroupThreads(2)
  public void parallel_2_tick$10rate$1_concurrentSkipListMapRateMeter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentSkipListMapRateMeter, bh, counter.v++, 10);
  }

  @Benchmark
  @Group("parallel_4_tick$10rate$1_concurrentSkipListMapRateMeter")
  @GroupThreads(4)
  public void parallel_4_tick$10rate$1_concurrentSkipListMapRateMeter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentSkipListMapRateMeter, bh, counter.v++, 10);
  }

  @Benchmark
  @Group("parallel_1_tick$1rate$1_concurrentSkipListMapRateMeter")
  @GroupThreads(1)
  public void parallel_1_tick$1rate$1_concurrentSkipListMapRateMeter_longAdderTicksCounter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentSkipListMapRateMeter, bh, counter.v++, 1);
  }

  @Benchmark
  @Group("parallel_2_tick$1rate$1_concurrentSkipListMapRateMeter")
  @GroupThreads(2)
  public void parallel_2_tick$1rate$1_concurrentSkipListMapRateMeter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentSkipListMapRateMeter, bh, counter.v++, 1);
  }

  @Benchmark
  @Group("parallel_4_tick$1rate$1_concurrentSkipListMapRateMeter")
  @GroupThreads(4)
  public void parallel_4_tick$1rate$1_concurrentSkipListMapRateMeter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentSkipListMapRateMeter, bh, counter.v++, 1);
  }

  @Benchmark
  @Group("parallel_1_tick$1rate$10_concurrentSkipListMapRateMeter")
  @GroupThreads(1)
  public void parallel_1_tick$1rate$10_concurrentSkipListMapRateMeter_longAdderTicksCounter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    rateAndTick(state.concurrentSkipListMapRateMeter, bh, counter.v++, 10);
  }

  @Benchmark
  @Group("parallel_2_tick$1rate$10_concurrentSkipListMapRateMeter")
  @GroupThreads(2)
  public void parallel_2_tick$1rate$10_concurrentSkipListMapRateMeter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    rateAndTick(state.concurrentSkipListMapRateMeter, bh, counter.v++, 10);
  }

  @Benchmark
  @Group("parallel_4_tick$1rate$10_concurrentSkipListMapRateMeter")
  @GroupThreads(4)
  public void parallel_4_tick$1rate$10_concurrentSkipListMapRateMeter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    rateAndTick(state.concurrentSkipListMapRateMeter, bh, counter.v++, 10);
  }

  @Benchmark
  @Group("parallel_4_tick$1_rate$1_concurrentSkipListMapRateMeter")
  @GroupThreads(2)
  public void parallel_2_TICK$1_rate$1_concurrentSkipListMapRateMeter(final RateMeterContainer_GroupScope state) {
    tick(state.concurrentSkipListMapRateMeter);
  }

  @Benchmark
  @Group("parallel_4_tick$1_rate$1_concurrentSkipListMapRateMeter")
  @GroupThreads(2)
  public void parallel_2_tick$1_RATE$1_concurrentSkipListMapRateMeter(final RateMeterContainer_GroupScope state, final Blackhole bh) {
    rate(state.concurrentSkipListMapRateMeter, bh);
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

  private static final void rateAndTick(final RateMeter rm, final Blackhole bh, final int counter, final int rateToTickRatio) {
    bh.consume(rm.rate());
    if (counter % rateToTickRatio == 0) {
      rm.tick(1, nanoTime());
    }
  }

  private static final long nanoTime() {
    return System.nanoTime();
  }

  @State(Scope.Thread)
  public static class RateMeterContainer_ThreadScope {
    TreeMapRateMeter treeMapRateMeter;
    ConcurrentSkipListMapRateMeter concurrentSkipListMapRateMeter;
    ArrayRateMeter arrayRateMeter;

    public RateMeterContainer_ThreadScope() {
    }

    @Setup(Level.Trial)
    public final void setup() {
      treeMapRateMeter = new TreeMapRateMeter(nanoTime(), samplesInterval,
          rateMeterConfigBuilderSuppplier.get()
              .setTicksCounterSupplier(LongTicksCounter::new)
              .build());
      concurrentSkipListMapRateMeter = new ConcurrentSkipListMapRateMeter(nanoTime(), samplesInterval,
          rateMeterConfigBuilderSuppplier.get()
              .setTicksCounterSupplier(LongTicksCounter::new)
              .build());
      arrayRateMeter = new ArrayRateMeter(nanoTime(), samplesInterval,
              rateMeterConfigBuilderSuppplier.get()
                      .setTicksCounterSupplier(LongTicksCounter::new)
                      .build());
    }
  }

  @State(Scope.Group)
  public static class RateMeterContainer_GroupScope {
    ConcurrentSkipListMapRateMeter concurrentSkipListMapRateMeter;

    public RateMeterContainer_GroupScope() {
    }

    @Setup(Level.Trial)
    public final void setup() {
      concurrentSkipListMapRateMeter = new ConcurrentSkipListMapRateMeter(nanoTime(), samplesInterval,
          rateMeterConfigBuilderSuppplier.get()
              .setTicksCounterSupplier(LongAdderTicksCounter::new)
              .build());
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