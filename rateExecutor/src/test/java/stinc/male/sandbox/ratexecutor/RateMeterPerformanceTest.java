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
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import stinc.male.PerformanceTest;
import stinc.male.sandbox.ratexecutor.RateMeterConfig.Builder;
import static org.junit.Assert.assertEquals;
import static org.openjdk.jmh.runner.options.TimeValue.milliseconds;

@Category(PerformanceTest.class)
public class RateMeterPerformanceTest {
  private static final Duration samplesInterval = Duration.ofMillis(100);
  private static final Duration timeSensitivity = Duration.ofMillis(10);
  private static final boolean SERVER = true;
  private static final boolean QUICK = true;
  private static final long ACCEPTABLE_FAILED_ACCURACY_EVENTS_COUNT = 0;
  private static final Supplier<ChainedOptionsBuilder> jmhOptionsBuilderSupplier = () -> {
    final ChainedOptionsBuilder result = new OptionsBuilder().mode(Mode.Throughput)
        .jvmArgsPrepend(SERVER ? "-server" : "-client")
        .timeUnit(TimeUnit.MILLISECONDS)
        .syncIterations(true)
        .shouldFailOnError(true)
        .shouldDoGC(true)
        .timeout(milliseconds(30_000));
    if (QUICK) {
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
  private static final Supplier<Builder> rateMeterConfigBuilderSuppplier = () -> RateMeterConfig.newBuilder()
      .setTimeSensitivity(timeSensitivity);

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
  public void serialNavigableMapRateMeter() throws RunnerException {
    new Runner(jmhOptionsBuilderSupplier.get()
        .include(getClass().getName() + ".serial_.*navigableMapRateMeter")
        .threads(1)
        .build())
        .run();
  }

  @Test
  public void serialConcurrentNavigableMapRateMeter() throws RunnerException {
    new Runner(jmhOptionsBuilderSupplier.get()
        .include(getClass().getName() + ".serial_.*concurrentNavigableMapRateMeter")
        .threads(1)
        .build())
        .run();
  }

  @Test
  public void serialRingBufferRateMeter() throws RunnerException {
    new Runner(jmhOptionsBuilderSupplier.get()
        .include(getClass().getName() + ".serial_.*ringBufferRateMeter")
        .threads(1)
        .build())
        .run();
  }

  @Test
  public void serialConcurrentRingBufferRateMeter() throws RunnerException {
    new Runner(jmhOptionsBuilderSupplier.get()
        .include(getClass().getName() + ".serial_.*concurrentRingBufferRateMeter")
        .threads(1)
        .build())
        .run();
  }

  @Test
  public void serialLinearizableRateMeter() throws RunnerException {
    new Runner(jmhOptionsBuilderSupplier.get()
        .include(getClass().getName() + ".serial_.*linearizableRateMeter")
        .threads(1)
        .build())
        .run();
  }

  @Test
  public void parallel_2_Baseline() throws RunnerException {
    new Runner(jmhOptionsBuilderSupplier.get()
        .include(RateMeterPerformanceTest.class.getName() + ".baseline_.*")
        .threads(2)
        .build())
        .run();
  }

  @Test
  public void parallel_4_Baseline() throws RunnerException {
    new Runner(jmhOptionsBuilderSupplier.get()
        .include(RateMeterPerformanceTest.class.getName() + ".baseline_.*")
        .threads(4)
        .build())
        .run();
  }

  @Test
  public void parallel_2_ConcurrentNavigableMapRateMeter() throws RunnerException {
    new Runner(jmhOptionsBuilderSupplier.get()
        .include(getClass().getName() + ".parallel_2.*concurrentNavigableMapRateMeter")
        .threads(2)
        .build())
        .run();
  }

  @Test
  public void parallel_4_ConcurrentNavigableMapRateMeter() throws RunnerException {
    new Runner(jmhOptionsBuilderSupplier.get()
        .include(getClass().getName() + ".parallel_4.*concurrentNavigableMapRateMeter")
        .threads(4)
        .build())
        .run();
  }

  @Test
  public void parallel_2_ConcurrentRingBufferRateMeterRateMeter() throws RunnerException {
    new Runner(jmhOptionsBuilderSupplier.get()
        .include(getClass().getName() + ".parallel_2.*concurrentRingBufferRateMeter")
        .threads(2)
        .build())
        .run();
  }

  @Test
  public void parallel_4_ConcurrentRingBufferRateMeterRateMeter() throws RunnerException {
    new Runner(jmhOptionsBuilderSupplier.get()
        .include(getClass().getName() + ".parallel_4.*concurrentRingBufferRateMeter")
        .threads(4)
        .build())
        .run();
  }

  @Test
  public void parallel_2_LinearizableRateMeter() throws RunnerException {
    new Runner(jmhOptionsBuilderSupplier.get()
        .include(getClass().getName() + ".parallel_2.*linearizableRateMete")
        .threads(2)
        .build())
        .run();
  }

  @Test
  public void parallel_4_LinearizableRateMeter() throws RunnerException {
    new Runner(jmhOptionsBuilderSupplier.get()
        .include(getClass().getName() + ".parallel_4.*linearizableRateMeter")
        .threads(4)
        .build())
        .run();
  }

  @Benchmark
  public void baseline_empty() {
  }

  @Benchmark
  public void baseline_blackhole_consume(final Blackhole bh) {
    bh.consume(0);
  }

  @Benchmark
  public void baseline_time(final Blackhole bh) {
    bh.consume(nanoTime());
  }

  @Benchmark
  public void baseline_intCounter(IntCounter_ThreadScope counter, final Blackhole bh) {
    counter.v++;
    bh.consume(counter.v % 10);
  }

  @Benchmark
  public void serial_tick_navigableMapRateMeter(final RateMeterContainer_ThreadScope state) {
    tick(state.navigableMapRateMeter);
  }

  @Benchmark
  public void serial_tick$10rate$1_navigableMapRateMeter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.navigableMapRateMeter, bh, counter.v++, 10);
  }

  @Benchmark
  public void serial_tick$1rate$1_navigableMapRateMeter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.navigableMapRateMeter, bh, counter.v++, 1);
  }

  @Benchmark
  public void serial_tick$1rate$10_navigableMapRateMeter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    rateAndTick(state.navigableMapRateMeter, bh, counter.v++, 10);
  }

  @Benchmark
  public void serial_tick_concurrentNavigableMapRateMeter(final RateMeterContainer_ThreadScope state) {
    tick(state.concurrentNavigableMapRateMeter);
  }

  @Benchmark
  public void serial_tick$10rate$1_concurrentNavigableMapRateMeter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentNavigableMapRateMeter, bh, counter.v++, 10);
  }

  @Benchmark
  public void serial_tick$1rate$1_concurrentNavigableMapRateMeter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentNavigableMapRateMeter, bh, counter.v++, 1);
  }

  @Benchmark
  public void serial_tick$1rate$10_concurrentNavigableMapRateMeter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    rateAndTick(state.concurrentNavigableMapRateMeter, bh, counter.v++, 10);
  }

  @Benchmark
  public void serial_tick_ringBufferRateMeter(final RateMeterContainer_ThreadScope state) {
    tick(state.ringBufferRateMeter);
  }

  @Benchmark
  public void serial_tick$10rate$1_ringBufferRateMeter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.ringBufferRateMeter, bh, counter.v++, 10);
  }

  @Benchmark
  public void serial_tick$1rate$1_ringBufferRateMeter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.ringBufferRateMeter, bh, counter.v++, 1);
  }

  @Benchmark
  public void serial_tick$1rate$10_ringBufferRateMeter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    rateAndTick(state.ringBufferRateMeter, bh, counter.v++, 10);
  }

  @Benchmark
  public void serial_tick_concurrentRingBufferRateMeter(final RateMeterContainer_ThreadScope state) {
    tick(state.concurrentRingBufferRateMeter);
  }

  @Benchmark
  public void serial_tick$10rate$1_concurrentRingBufferRateMeter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentRingBufferRateMeter, bh, counter.v++, 10);
  }

  @Benchmark
  public void serial_tick$1rate$1_concurrentRingBufferRateMeter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentRingBufferRateMeter, bh, counter.v++, 1);
  }

  @Benchmark
  public void serial_tick$1rate$10_concurrentRingBufferRateMeter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    rateAndTick(state.concurrentRingBufferRateMeter, bh, counter.v++, 10);
  }

  @Benchmark
  public void serial_tick_linearizableRateMeter(final RateMeterContainer_ThreadScope state) {
    tick(state.linearizableRateMeter);
  }

  @Benchmark
  public void serial_tick$10rate$1_linearizableRateMeter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.linearizableRateMeter, bh, counter.v++, 10);
  }

  @Benchmark
  public void serial_tick$1rate$1_linearizableRateMeter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.linearizableRateMeter, bh, counter.v++, 1);
  }

  @Benchmark
  public void serial_tick$1rate$10_linearizableRateMeter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    rateAndTick(state.linearizableRateMeter, bh, counter.v++, 10);
  }

  @Benchmark
  @Group("parallel_2_tick_concurrentNavigableMapRateMeter")
  @GroupThreads(2)
  public void parallel_2_tick_concurrentNavigableMapRateMeter(final RateMeterContainer_GroupScope state) {
    tick(state.concurrentNavigableMapRateMeter);
  }

  @Benchmark
  @Group("parallel_4_tick_concurrentNavigableMapRateMeter")
  @GroupThreads(4)
  public void parallel_4_tick_concurrentNavigableMapRateMeter(final RateMeterContainer_GroupScope state) {
    tick(state.concurrentNavigableMapRateMeter);
  }

  @Benchmark
  @Group("parallel_2_tick$10rate$1_concurrentNavigableMapRateMeter")
  @GroupThreads(2)
  public void parallel_2_tick$10rate$1_concurrentNavigableMapRateMeter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentNavigableMapRateMeter, bh, counter.v++, 10);
  }

  @Benchmark
  @Group("parallel_4_tick$10rate$1_concurrentNavigableMapRateMeter")
  @GroupThreads(4)
  public void parallel_4_tick$10rate$1_concurrentNavigableMapRateMeter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentNavigableMapRateMeter, bh, counter.v++, 10);
  }

  @Benchmark
  @Group("parallel_2_tick$1rate$1_concurrentNavigableMapRateMeter")
  @GroupThreads(2)
  public void parallel_2_tick$1rate$1_concurrentNavigableMapRateMeter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentNavigableMapRateMeter, bh, counter.v++, 1);
  }

  @Benchmark
  @Group("parallel_4_tick$1rate$1_concurrentNavigableMapRateMeter")
  @GroupThreads(4)
  public void parallel_4_tick$1rate$1_concurrentNavigableMapRateMeter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentNavigableMapRateMeter, bh, counter.v++, 1);
  }

  @Benchmark
  @Group("parallel_2_tick$1rate$10_concurrentNavigableMapRateMeter")
  @GroupThreads(2)
  public void parallel_2_tick$1rate$10_concurrentNavigableMapRateMeter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    rateAndTick(state.concurrentNavigableMapRateMeter, bh, counter.v++, 10);
  }

  @Benchmark
  @Group("parallel_4_tick$1rate$10_concurrentNavigableMapRateMeter")
  @GroupThreads(4)
  public void parallel_4_tick$1rate$10_concurrentNavigableMapRateMeter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    rateAndTick(state.concurrentNavigableMapRateMeter, bh, counter.v++, 10);
  }

  @Benchmark
  @Group("parallel_4_tick$1_rate$1_concurrentNavigableMapRateMeter")
  @GroupThreads(2)
  public void parallel_4_TICK$1_rate$1_concurrentNavigableMapRateMeter(final RateMeterContainer_GroupScope state) {
    tick(state.concurrentNavigableMapRateMeter);
  }

  @Benchmark
  @Group("parallel_4_tick$1_rate$1_concurrentNavigableMapRateMeter")
  @GroupThreads(2)
  public void parallel_4_tick$1_RATE$1_concurrentNavigableMapRateMeter(final RateMeterContainer_GroupScope state, final Blackhole bh) {
    rate(state.concurrentNavigableMapRateMeter, bh);
  }

  @Benchmark
  @Group("parallel_2_tick_concurrentRingBufferRateMeter")
  @GroupThreads(2)
  public void parallel_2_tick_concurrentRingBufferRateMeter(final RateMeterContainer_GroupScope state) {
    tick(state.concurrentRingBufferRateMeter);
  }

  @Benchmark
  @Group("parallel_4_tick_concurrentRingBufferRateMeter")
  @GroupThreads(4)
  public void parallel_4_tick_concurrentRingBufferRateMeter(final RateMeterContainer_GroupScope state) {
    tick(state.concurrentRingBufferRateMeter);
  }

  @Benchmark
  @Group("parallel_2_tick$10rate$1_concurrentRingBufferRateMeter")
  @GroupThreads(2)
  public void parallel_2_tick$10rate$1_concurrentRingBufferRateMeter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentRingBufferRateMeter, bh, counter.v++, 10);
  }

  @Benchmark
  @Group("parallel_4_tick$10rate$1_concurrentRingBufferRateMeter")
  @GroupThreads(4)
  public void parallel_4_tick$10rate$1_concurrentRingBufferRateMeter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentRingBufferRateMeter, bh, counter.v++, 10);
  }

  @Benchmark
  @Group("parallel_2_tick$1rate$1_concurrentRingBufferRateMeter")
  @GroupThreads(2)
  public void parallel_2_tick$1rate$1_concurrentRingBufferRateMeter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentRingBufferRateMeter, bh, counter.v++, 1);
  }

  @Benchmark
  @Group("parallel_4_tick$1rate$1_concurrentRingBufferRateMeter")
  @GroupThreads(4)
  public void parallel_4_tick$1rate$1_concurrentRingBufferRateMeter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentRingBufferRateMeter, bh, counter.v++, 1);
  }

  @Benchmark
  @Group("parallel_2_tick$1rate$10_concurrentRingBufferRateMeter")
  @GroupThreads(2)
  public void parallel_2_tick$1rate$10_concurrentRingBufferRateMeter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    rateAndTick(state.concurrentRingBufferRateMeter, bh, counter.v++, 10);
  }

  @Benchmark
  @Group("parallel_4_tick$1rate$10_concurrentRingBufferRateMeter")
  @GroupThreads(4)
  public void parallel_4_tick$1rate$10_concurrentRingBufferRateMeter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    rateAndTick(state.concurrentRingBufferRateMeter, bh, counter.v++, 10);
  }

  @Benchmark
  @Group("parallel_4_tick$1_rate$1_concurrentRingBufferRateMeter")
  @GroupThreads(2)
  public void parallel_4_TICK$1_rate$1_concurrentRingBufferRateMeter(final RateMeterContainer_GroupScope state) {
    tick(state.concurrentRingBufferRateMeter);
  }

  @Benchmark
  @Group("parallel_4_tick$1_rate$1_concurrentRingBufferRateMeter")
  @GroupThreads(2)
  public void parallel_4_tick$1_RATE$1_concurrentRingBufferRateMeter(final RateMeterContainer_GroupScope state, final Blackhole bh) {
    rate(state.concurrentRingBufferRateMeter, bh);
  }

  @Benchmark
  @Group("parallel_2_tick_linearizableRateMeter")
  @GroupThreads(2)
  public void parallel_2_tick_linearizableRateMeter(final RateMeterContainer_GroupScope state) {
    tick(state.linearizableRateMeter);
  }

  @Benchmark
  @Group("parallel_4_tick_linearizableRateMeter")
  @GroupThreads(4)
  public void parallel_4_tick_linearizableRateMeter(final RateMeterContainer_GroupScope state) {
    tick(state.linearizableRateMeter);
  }

  @Benchmark
  @Group("parallel_2_tick$10rate$1_linearizableRateMeter")
  @GroupThreads(2)
  public void parallel_2_tick$10rate$1_linearizableRateMeter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.linearizableRateMeter, bh, counter.v++, 10);
  }

  @Benchmark
  @Group("parallel_4_tick$10rate$1_linearizableRateMeter")
  @GroupThreads(4)
  public void parallel_4_tick$10rate$1_linearizableRateMeter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.linearizableRateMeter, bh, counter.v++, 10);
  }

  @Benchmark
  @Group("parallel_2_tick$1rate$1_linearizableRateMeter")
  @GroupThreads(2)
  public void parallel_2_tick$1rate$1_linearizableRateMeter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.linearizableRateMeter, bh, counter.v++, 1);
  }

  @Benchmark
  @Group("parallel_4_tick$1rate$1_linearizableRateMeter")
  @GroupThreads(4)
  public void parallel_4_tick$1rate$1_linearizableRateMeter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.linearizableRateMeter, bh, counter.v++, 1);
  }

  @Benchmark
  @Group("parallel_2_tick$1rate$10_linearizableRateMeter")
  @GroupThreads(2)
  public void parallel_2_tick$1rate$10_linearizableRateMeter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    rateAndTick(state.linearizableRateMeter, bh, counter.v++, 10);
  }

  @Benchmark
  @Group("parallel_4_tick$1rate$10_linearizableRateMeter")
  @GroupThreads(4)
  public void parallel_4_tick$1rate$10_linearizableRateMeter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    rateAndTick(state.linearizableRateMeter, bh, counter.v++, 10);
  }

  @Benchmark
  @Group("parallel_4_tick$1_rate$1_linearizableRateMeter")
  @GroupThreads(2)
  public void parallel_4_TICK$1_rate$1_linearizableRateMeter(final RateMeterContainer_GroupScope state) {
    tick(state.linearizableRateMeter);
  }

  @Benchmark
  @Group("parallel_4_tick$1_rate$1_linearizableRateMeter")
  @GroupThreads(2)
  public void parallel_4_tick$1_RATE$1_linearizableRateMeter(final RateMeterContainer_GroupScope state, final Blackhole bh) {
    rate(state.linearizableRateMeter, bh);
  }

  private static final void tick(final RateMeter rm) {
    rm.tick(1, nanoTime());
  }

  private static final void rate(final RateMeter rm, final Blackhole bh) {
    bh.consume(rm.rate());
//    bh.consume(rm.rate(nanoTime()));
//    bh.consume(rm.rate(rm.rightSamplesWindowBoundary()));
  }

  private static final void tickAndRate(final RateMeter rm, final Blackhole bh, final int counter, final int tickToRateRatio) {
    if (counter % (tickToRateRatio + 1) == 0) {
      rate(rm, bh);
    } else {
      rm.tick(1, nanoTime());
    }
  }

  private static final void rateAndTick(final RateMeter rm, final Blackhole bh, final int counter, final int rateToTickRatio) {
    if (counter % (rateToTickRatio + 1) == 0) {
      rm.tick(1, nanoTime());
    } else {
      rate(rm, bh);
    }
  }

  private static final long nanoTime() {
    return System.nanoTime();
//    return TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis());
  }

  @State(Scope.Thread)
  public static class RateMeterContainer_ThreadScope {
    NavigableMapRateMeter navigableMapRateMeter;
    ConcurrentNavigableMapRateMeter concurrentNavigableMapRateMeter;
    RingBufferRateMeter ringBufferRateMeter;
    ConcurrentRingBufferRateMeter concurrentRingBufferRateMeter;
    LinearizableRateMeter linearizableRateMeter;

    public RateMeterContainer_ThreadScope() {
    }

    @Setup(Level.Trial)
    public final void setup() {
      navigableMapRateMeter = new NavigableMapRateMeter(nanoTime(), samplesInterval,
          rateMeterConfigBuilderSuppplier.get()
              .setTicksCounterSupplier(LongTicksCounter::new)
              .build());
      concurrentNavigableMapRateMeter = new ConcurrentNavigableMapRateMeter(nanoTime(), samplesInterval,
          rateMeterConfigBuilderSuppplier.get()
              .setTicksCounterSupplier(LongTicksCounter::new)
              .build());
      ringBufferRateMeter = new RingBufferRateMeter(nanoTime(), samplesInterval,
          rateMeterConfigBuilderSuppplier.get()
              .setTicksCounterSupplier(LongTicksCounter::new)
              .build());
      concurrentRingBufferRateMeter = new ConcurrentRingBufferRateMeter(nanoTime(), samplesInterval,
          ConcurrentRingBufferRateMeterConfig.newBuilder(rateMeterConfigBuilderSuppplier.get().build())
              .setTicksCounterSupplier(LongAdderTicksCounter::new)
              .build());
      linearizableRateMeter = new LinearizableRateMeter(
          new RingBufferRateMeter(nanoTime(), samplesInterval,
              rateMeterConfigBuilderSuppplier.get()
                  .setTicksCounterSupplier(LongTicksCounter::new)
                  .build()));
    }
  }

  @State(Scope.Group)
  public static class RateMeterContainer_GroupScope {
    ConcurrentNavigableMapRateMeter concurrentNavigableMapRateMeter;
    ConcurrentRingBufferRateMeter concurrentRingBufferRateMeter;
    LinearizableRateMeter linearizableRateMeter;

    public RateMeterContainer_GroupScope() {
    }

    @Setup(Level.Trial)
    public final void setup() {
      concurrentNavigableMapRateMeter = new ConcurrentNavigableMapRateMeter(nanoTime(), samplesInterval,
          rateMeterConfigBuilderSuppplier.get()
              .setTicksCounterSupplier(LongAdderTicksCounter::new)
              .build());
      concurrentRingBufferRateMeter = new ConcurrentRingBufferRateMeter(nanoTime(), samplesInterval,
          ConcurrentRingBufferRateMeterConfig.newBuilder(rateMeterConfigBuilderSuppplier.get().build())
              .setTicksCounterSupplier(LongAdderTicksCounter::new)
              .build());
      linearizableRateMeter = new LinearizableRateMeter(
          new RingBufferRateMeter(nanoTime(), samplesInterval,
              rateMeterConfigBuilderSuppplier.get()
                  .setTicksCounterSupplier(LongTicksCounter::new)
                  .build()));
    }

    @TearDown(Level.Trial)
    public final void tearDown() {
      assertEquals(0, concurrentNavigableMapRateMeter.stats().failedAccuracyEventsCountForTicksCount(), ACCEPTABLE_FAILED_ACCURACY_EVENTS_COUNT);
      assertEquals(0, concurrentNavigableMapRateMeter.stats().failedAccuracyEventsCountForTick(), ACCEPTABLE_FAILED_ACCURACY_EVENTS_COUNT);
      assertEquals(0, concurrentNavigableMapRateMeter.stats().failedAccuracyEventsCountForRateAverage(), ACCEPTABLE_FAILED_ACCURACY_EVENTS_COUNT);
      assertEquals(0, concurrentNavigableMapRateMeter.stats().failedAccuracyEventsCountForRate(), ACCEPTABLE_FAILED_ACCURACY_EVENTS_COUNT);
      assertEquals(0, concurrentRingBufferRateMeter.stats().failedAccuracyEventsCountForTicksCount(), ACCEPTABLE_FAILED_ACCURACY_EVENTS_COUNT);
      assertEquals(0, concurrentRingBufferRateMeter.stats().failedAccuracyEventsCountForTick(), ACCEPTABLE_FAILED_ACCURACY_EVENTS_COUNT);
      assertEquals(0, concurrentRingBufferRateMeter.stats().failedAccuracyEventsCountForRateAverage(), ACCEPTABLE_FAILED_ACCURACY_EVENTS_COUNT);
      assertEquals(0, concurrentRingBufferRateMeter.stats().failedAccuracyEventsCountForRate(), ACCEPTABLE_FAILED_ACCURACY_EVENTS_COUNT);
      assertEquals(0, linearizableRateMeter.stats().failedAccuracyEventsCountForTicksCount(), ACCEPTABLE_FAILED_ACCURACY_EVENTS_COUNT);
      assertEquals(0, linearizableRateMeter.stats().failedAccuracyEventsCountForTick(), ACCEPTABLE_FAILED_ACCURACY_EVENTS_COUNT);
      assertEquals(0, linearizableRateMeter.stats().failedAccuracyEventsCountForRateAverage(), ACCEPTABLE_FAILED_ACCURACY_EVENTS_COUNT);
      assertEquals(0, linearizableRateMeter.stats().failedAccuracyEventsCountForRate(), ACCEPTABLE_FAILED_ACCURACY_EVENTS_COUNT);
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