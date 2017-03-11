package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
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
import stinc.male.test.harness.PerformanceTest;
import stinc.male.sandbox.ratexecutor.RateMeterConfig.Builder;
import static org.junit.Assert.assertEquals;
import static org.openjdk.jmh.runner.options.TimeValue.milliseconds;

@Category(PerformanceTest.class)
public class RateMeterPerformanceTest {
  private static final Duration samplesInterval = Duration.of(1, ChronoUnit.MILLIS);
  private static final Duration timeSensitivity = Duration.of(50, ChronoUnit.MICROS);
  private static final boolean SERVER = true;
  private static final boolean QUICK = false;
  private static final long ACCEPTABLE_FAILED_ACCURACY_EVENTS_COUNT_PER_TRIAL = 0;
  private static final Supplier<ChainedOptionsBuilder> jmhOptionsBuilderSupplier = () -> {
    final ChainedOptionsBuilder result = new OptionsBuilder()
        .jvmArgsPrepend(SERVER ? "-server" : "-client")
        .syncIterations(true)
        .shouldFailOnError(true)
        .shouldDoGC(true)
        .timeout(milliseconds(30_000));
    if (QUICK) {
      result.warmupTime(milliseconds(samplesInterval.toMillis() * 3))
          .warmupIterations(1)
          .measurementTime(milliseconds(100))
          .measurementIterations(1)
          .forks(1);
    } else {
      result.warmupTime(milliseconds(750))
          .warmupIterations(3)
          .measurementTime(milliseconds(1000))
          .measurementIterations(3)
          .forks(3);
    }
    return result;
  };
  private static final Supplier<WaitStrategy> waitStrategySupplier = () -> YieldWaitStrategy.instance();
  private static final Supplier<LockingStrategy> lockingStrategySupplier = () -> new SpinLockingStrategy(waitStrategySupplier.get());
  private static final Supplier<Builder> rateMeterConfigBuilderSuppplier = () -> RateMeterConfig.newBuilder()
      .setCollectStats(true)
      .setTimeSensitivity(timeSensitivity);

  public RateMeterPerformanceTest() {
  }

  @Test
  public void serial_throughput_baseline() throws RunnerException {
    new Runner(jmhOptionsBuilderSupplier.get()
        .mode(Mode.Throughput)
        .timeUnit(TimeUnit.MILLISECONDS)
        .include(getClass().getName() + ".*baseline_.*")
        .threads(1)
        .build())
        .run();
  }

  @Test
  public void serial_throughput_navigableMapRateMeter() throws RunnerException {
    new Runner(jmhOptionsBuilderSupplier.get()
        .mode(Mode.Throughput)
        .timeUnit(TimeUnit.MILLISECONDS)
        .include(getClass().getName() + ".*serial_.*navigableMapRateMeter")
        .threads(1)
        .build())
        .run();
  }

  @Test
  public void serial_throughput_concurrentNavigableMapRateMeter() throws RunnerException {
    new Runner(jmhOptionsBuilderSupplier.get()
        .mode(Mode.Throughput)
        .timeUnit(TimeUnit.MILLISECONDS)
        .include(getClass().getName() + ".*serial_.*concurrentNavigableMapRateMeter")
        .threads(1)
        .build())
        .run();
  }

  @Test
  public void serial_throughput_ringBufferRateMeter() throws RunnerException {
    new Runner(jmhOptionsBuilderSupplier.get()
        .mode(Mode.Throughput)
        .timeUnit(TimeUnit.MILLISECONDS)
        .include(getClass().getName() + ".*serial_.*ringBufferRateMeter")
        .threads(1)
        .build())
        .run();
  }

  @Test
  public void serial_throughput_concurrentRingBufferRateMeter() throws RunnerException {
    new Runner(jmhOptionsBuilderSupplier.get()
        .mode(Mode.Throughput)
        .timeUnit(TimeUnit.MILLISECONDS)
        .include(getClass().getName() + ".*serial_.*concurrentRingBufferRateMeter")
        .threads(1)
        .build())
        .run();
  }

  @Test
  public void serial_throughput_concurrentSimpleRateMeter() throws RunnerException {
    new Runner(jmhOptionsBuilderSupplier.get()
        .mode(Mode.Throughput)
        .timeUnit(TimeUnit.MILLISECONDS)
        .include(getClass().getName() + ".*serial_.*concurrentSimpleRateMeter")
        .threads(1)
        .build())
        .run();
  }

  @Test
  public void serial_latency_concurrentNavigableMapRateMeter() throws RunnerException {
    new Runner(jmhOptionsBuilderSupplier.get()
        .mode(Mode.AverageTime)
        .timeUnit(TimeUnit.NANOSECONDS)
        .include(getClass().getName() + ".*serial_tick_concurrentNavigableMapRateMeter")
        .include(getClass().getName() + ".*serial_tick\\$1rate\\$10_concurrentNavigableMapRateMeter")
        .threads(1)
        .build())
        .run();
  }

  @Test
  public void serial_latency_concurrentRingBufferRateMeter() throws RunnerException {
    new Runner(jmhOptionsBuilderSupplier.get()
        .mode(Mode.AverageTime)
        .timeUnit(TimeUnit.NANOSECONDS)
        .include(getClass().getName() + ".*serial_tick_concurrentRingBufferRateMeter")
        .include(getClass().getName() + ".*serial_tick\\$1rate\\$10_concurrentRingBufferRateMeter")
        .threads(1)
        .build())
        .run();
  }

  @Test
  public void serial_latency_concurrentSimpleRateMeter() throws RunnerException {
    new Runner(jmhOptionsBuilderSupplier.get()
        .mode(Mode.AverageTime)
        .timeUnit(TimeUnit.NANOSECONDS)
        .include(getClass().getName() + ".*serial_tick_concurrentSimpleRateMeter")
        .include(getClass().getName() + ".*serial_tick\\$1rate\\$10_concurrentSimpleRateMeter")
        .threads(1)
        .build())
        .run();
  }

  @Test
  public void parallel4_throughput_baseline() throws RunnerException {
    new Runner(jmhOptionsBuilderSupplier.get()
        .mode(Mode.Throughput)
        .timeUnit(TimeUnit.MILLISECONDS)
        .include(RateMeterPerformanceTest.class.getName() + ".baseline_.*")
        .threads(4)
        .build())
        .run();
  }

  @Test
  public void parallel4_throughput_concurrentNavigableMapRateMeter() throws RunnerException {
    new Runner(jmhOptionsBuilderSupplier.get()
        .mode(Mode.Throughput)
        .timeUnit(TimeUnit.MILLISECONDS)
        .include(getClass().getName() + ".parallel4.*concurrentNavigableMapRateMeter")
        .threads(4)
        .build())
        .run();
  }

  @Test
  public void parallel4_throughput_concurrentRingBufferRateMeter() throws RunnerException {
    new Runner(jmhOptionsBuilderSupplier.get()
        .mode(Mode.Throughput)
        .timeUnit(TimeUnit.MILLISECONDS)
        .include(getClass().getName() + ".parallel4.*concurrentRingBufferRateMeter")
        .threads(4)
        .build())
        .run();
  }

  @Test
  public void parallel4_throughput_concurrentSimpleRateMeter() throws RunnerException {
    new Runner(jmhOptionsBuilderSupplier.get()
        .mode(Mode.Throughput)
        .timeUnit(TimeUnit.MILLISECONDS)
        .include(getClass().getName() + ".parallel4.*concurrentSimpleRateMeter")
        .threads(4)
        .build())
        .run();
  }

  @Test
  public void parallel4_latency_concurrentNavigableMapRateMeter() throws RunnerException {
    new Runner(jmhOptionsBuilderSupplier.get()
        .mode(Mode.AverageTime)
        .timeUnit(TimeUnit.NANOSECONDS)
        .include(getClass().getName() + ".*parallel4_tick_concurrentNavigableMapRateMeter")
        .include(getClass().getName() + ".*parallel4_tick\\$1_rate\\$3_concurrentNavigableMapRateMeter")
        .include(getClass().getName() + ".*parallel4_tick\\$1_rate\\$1_concurrentNavigableMapRateMeter")
        .threads(4)
        .build())
        .run();
  }

  @Test
  public void parallel4_latency_concurrentRingBufferRateMeter() throws RunnerException {
    new Runner(jmhOptionsBuilderSupplier.get()
        .mode(Mode.AverageTime)
        .timeUnit(TimeUnit.NANOSECONDS)
        .include(getClass().getName() + ".*parallel4_tick_concurrentRingBufferRateMeter")
        .include(getClass().getName() + ".*parallel4_tick\\$1_rate\\$3_concurrentRingBufferRateMeter")
        .include(getClass().getName() + ".*parallel4_tick\\$1_rate\\$1_concurrentRingBufferRateMeter")
        .threads(4)
        .build())
        .run();
  }

  @Test
  public void parallel4_latency_concurrentSimpleRateMeter() throws RunnerException {
    new Runner(jmhOptionsBuilderSupplier.get()
        .mode(Mode.AverageTime)
        .timeUnit(TimeUnit.NANOSECONDS)
        .include(getClass().getName() + ".*parallel4_tick_concurrentSimpleRateMeter")
        .include(getClass().getName() + ".*parallel4_tick\\$1_rate\\$3_concurrentSimpleRateMeter")
        .include(getClass().getName() + ".*parallel4_tick\\$1_rate\\$1_concurrentSimpleRateMeter")
        .threads(4)
        .build())
        .run();
  }

  @Benchmark
  public void baseline_empty() {
  }

  @Benchmark
  public void baseline_consume_blackhole(final Blackhole bh) {
    bh.consume(0);
  }

  @Benchmark
  public void baseline_time(final Blackhole bh) {//for latency and granularity see https://github.com/shipilev/timers-bench
    bh.consume(nanoTime());
  }

  @Benchmark
  @Group("baseline_intCounter")
  @GroupThreads(1)
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
  public void serial_tick_concurrentSimpleRateMeter(final RateMeterContainer_ThreadScope state) {
    tick(state.concurrentSimpleRateMeter);
  }

  @Benchmark
  public void serial_tick$10rate$1_concurrentSimpleRateMeter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentSimpleRateMeter, bh, counter.v++, 10);
  }

  @Benchmark
  public void serial_tick$1rate$1_concurrentSimpleRateMeter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentSimpleRateMeter, bh, counter.v++, 1);
  }

  @Benchmark
  public void serial_tick$1rate$10_concurrentSimpleRateMeter(final RateMeterContainer_ThreadScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    rateAndTick(state.concurrentSimpleRateMeter, bh, counter.v++, 10);
  }

  @Benchmark
  @Group("parallel4_tick_concurrentNavigableMapRateMeter")
  @GroupThreads(4)
  public void parallel4_tick_concurrentNavigableMapRateMeter(final RateMeterContainer_GroupScope state) {
    tick(state.concurrentNavigableMapRateMeter);
  }

  @Benchmark
  @Group("parallel4_tick$10rate$1_concurrentNavigableMapRateMeter")
  @GroupThreads(4)
  public void parallel4_tick$10rate$1_concurrentNavigableMapRateMeter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentNavigableMapRateMeter, bh, counter.v++, 10);
  }

  @Benchmark
  @Group("parallel4_tick$1rate$1_concurrentNavigableMapRateMeter")
  @GroupThreads(4)
  public void parallel4_tick$1rate$1_concurrentNavigableMapRateMeter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentNavigableMapRateMeter, bh, counter.v++, 1);
  }

  @Benchmark
  @Group("parallel4_tick$1rate$10_concurrentNavigableMapRateMeter")
  @GroupThreads(4)
  public void parallel4_tick$1rate$10_concurrentNavigableMapRateMeter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    rateAndTick(state.concurrentNavigableMapRateMeter, bh, counter.v++, 10);
  }

  @Benchmark
  @Group("parallel4_tick$1_rate$1_concurrentNavigableMapRateMeter")
  @GroupThreads(2)
  public void parallel4_TICK$1_rate$1_concurrentNavigableMapRateMeter(final RateMeterContainer_GroupScope state) {
    tick(state.concurrentNavigableMapRateMeter);
  }

  @Benchmark
  @Group("parallel4_tick$1_rate$1_concurrentNavigableMapRateMeter")
  @GroupThreads(2)
  public void parallel4_tick$1_RATE$1_concurrentNavigableMapRateMeter(final RateMeterContainer_GroupScope state, final Blackhole bh) {
    rate(state.concurrentNavigableMapRateMeter, bh);
  }

  @Benchmark
  @Group("parallel4_tick$1_rate$3_concurrentNavigableMapRateMeter")
  @GroupThreads(1)
  public void parallel4_TICK$1_rate$3_concurrentNavigableMapRateMeter(final RateMeterContainer_GroupScope state) {
    tick(state.concurrentNavigableMapRateMeter);
  }

  @Benchmark
  @Group("parallel4_tick$1_rate$3_concurrentNavigableMapRateMeter")
  @GroupThreads(3)
  public void parallel4_tick$1_RATE$3_concurrentNavigableMapRateMeter(final RateMeterContainer_GroupScope state, final Blackhole bh) {
    rate(state.concurrentNavigableMapRateMeter, bh);
  }

  @Benchmark
  @Group("parallel4_tick_concurrentRingBufferRateMeter")
  @GroupThreads(4)
  public void parallel4_tick_concurrentRingBufferRateMeter(final RateMeterContainer_GroupScope state) {
    tick(state.concurrentRingBufferRateMeter);
  }

  @Benchmark
  @Group("parallel4_tick$10rate$1_concurrentRingBufferRateMeter")
  @GroupThreads(4)
  public void parallel4_tick$10rate$1_concurrentRingBufferRateMeter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentRingBufferRateMeter, bh, counter.v++, 10);
  }

  @Benchmark
  @Group("parallel4_tick$1rate$1_concurrentRingBufferRateMeter")
  @GroupThreads(4)
  public void parallel4_tick$1rate$1_concurrentRingBufferRateMeter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentRingBufferRateMeter, bh, counter.v++, 1);
  }

  @Benchmark
  @Group("parallel4_tick$1rate$10_concurrentRingBufferRateMeter")
  @GroupThreads(4)
  public void parallel4_tick$1rate$10_concurrentRingBufferRateMeter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    rateAndTick(state.concurrentRingBufferRateMeter, bh, counter.v++, 10);
  }

  @Benchmark
  @Group("parallel4_tick$1_rate$1_concurrentRingBufferRateMeter")
  @GroupThreads(2)
  public void parallel4_TICK$1_rate$1_concurrentRingBufferRateMeter(final RateMeterContainer_GroupScope state) {
    tick(state.concurrentRingBufferRateMeter);
  }

  @Benchmark
  @Group("parallel4_tick$1_rate$1_concurrentRingBufferRateMeter")
  @GroupThreads(2)
  public void parallel4_tick$1_RATE$1_concurrentRingBufferRateMeter(final RateMeterContainer_GroupScope state, final Blackhole bh) {
    rate(state.concurrentRingBufferRateMeter, bh);
  }

  @Benchmark
  @Group("parallel4_tick$1_rate$3_concurrentRingBufferRateMeter")
  @GroupThreads(1)
  public void parallel4_TICK$1_rate$3_concurrentRingBufferRateMeter(final RateMeterContainer_GroupScope state) {
    tick(state.concurrentRingBufferRateMeter);
  }

  @Benchmark
  @Group("parallel4_tick$1_rate$3_concurrentRingBufferRateMeter")
  @GroupThreads(3)
  public void parallel4_tick$1_RATE$3_concurrentRingBufferRateMeter(final RateMeterContainer_GroupScope state, final Blackhole bh) {
    rate(state.concurrentRingBufferRateMeter, bh);
  }

  @Benchmark
  @Group("parallel4_tick_concurrentSimpleRateMeter")
  @GroupThreads(4)
  public void parallel4_tick_concurrentSimpleRateMeter(final RateMeterContainer_GroupScope state) {
    tick(state.concurrentSimpleRateMeter);
  }

  @Benchmark
  @Group("parallel4_tick$10rate$1_concurrentSimpleRateMeter")
  @GroupThreads(4)
  public void parallel4_tick$10rate$1_concurrentSimpleRateMeter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentSimpleRateMeter, bh, counter.v++, 10);
  }

  @Benchmark
  @Group("parallel4_tick$1rate$1_concurrentSimpleRateMeter")
  @GroupThreads(4)
  public void parallel4_tick$1rate$1_concurrentSimpleRateMeter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    tickAndRate(state.concurrentSimpleRateMeter, bh, counter.v++, 1);
  }

  @Benchmark
  @Group("parallel4_tick$1rate$10_concurrentSimpleRateMeter")
  @GroupThreads(4)
  public void parallel4_tick$1rate$10_concurrentSimpleRateMeter(final RateMeterContainer_GroupScope state, final IntCounter_ThreadScope counter, final Blackhole bh) {
    rateAndTick(state.concurrentSimpleRateMeter, bh, counter.v++, 10);
  }

  @Benchmark
  @Group("parallel4_tick$1_rate$1_concurrentSimpleRateMeter")
  @GroupThreads(2)
  public void parallel4_TICK$1_rate$1_concurrentSimpleRateMeter(final RateMeterContainer_GroupScope state) {
    tick(state.concurrentSimpleRateMeter);
  }

  @Benchmark
  @Group("parallel4_tick$1_rate$1_concurrentSimpleRateMeter")
  @GroupThreads(2)
  public void parallel4_tick$1_RATE$1_concurrentSimpleRateMeter(final RateMeterContainer_GroupScope state, final Blackhole bh) {
    rate(state.concurrentSimpleRateMeter, bh);
  }

  @Benchmark
  @Group("parallel4_tick$1_rate$3_concurrentSimpleRateMeter")
  @GroupThreads(1)
  public void parallel4_TICK$1_rate$3_concurrentSimpleRateMeter(final RateMeterContainer_GroupScope state) {
    tick(state.concurrentSimpleRateMeter);
  }

  @Benchmark
  @Group("parallel4_tick$1_rate$3_concurrentSimpleRateMeter")
  @GroupThreads(3)
  public void parallel4_tick$1_RATE$3_concurrentSimpleRateMeter(final RateMeterContainer_GroupScope state, final Blackhole bh) {
    rate(state.concurrentSimpleRateMeter, bh);
  }

  private static final void tick(final RateMeter rm) {
    rm.tick(1, nanoTime());
  }

  private static final void rate(final RateMeter rm, final Blackhole bh) {
    bh.consume(rm.rate());
//    bh.consume(rm.rate(new RateMeterReading()));
//    bh.consume(rm.rate(samplesInterval));
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
    ConcurrentSimpleRateMeter concurrentSimpleRateMeter;

    public RateMeterContainer_ThreadScope() {
    }

    @Setup(Level.Trial)
    public final void setup() {
      navigableMapRateMeter = new NavigableMapRateMeter(nanoTime(), samplesInterval,
          rateMeterConfigBuilderSuppplier.get()
              .setTicksCounterSupplier(LongTicksCounter::new)
              .setHl(2)
              .build());
      concurrentNavigableMapRateMeter = new ConcurrentNavigableMapRateMeter(nanoTime(), samplesInterval,
          rateMeterConfigBuilderSuppplier.get()
              .setTicksCounterSupplier(LongAdderTicksCounter::new)
              .setHl(3)
              .build());
      ringBufferRateMeter = new RingBufferRateMeter(nanoTime(), samplesInterval,
          rateMeterConfigBuilderSuppplier.get()
              .setTicksCounterSupplier(LongTicksCounter::new)
              .setHl(2)
              .build());
      final ConcurrentRingBufferRateMeterConfig.Builder concurrentRingBufferRateMeterConfigBuilder
          = ConcurrentRingBufferRateMeterConfig.newBuilder(rateMeterConfigBuilderSuppplier.get().build());
      concurrentRingBufferRateMeterConfigBuilder.setStrictTick(true)
          .setWaitStrategySupplier(waitStrategySupplier)
          .setLockStrategySupplier(lockingStrategySupplier)
          .setTicksCounterSupplier(LongAdderTicksCounter::new)
          .setHl(20);
      concurrentRingBufferRateMeter = new ConcurrentRingBufferRateMeter(nanoTime(), samplesInterval,
          concurrentRingBufferRateMeterConfigBuilder.build());
      concurrentSimpleRateMeter = new ConcurrentSimpleRateMeter(
          new RingBufferRateMeter(nanoTime(), samplesInterval,
              rateMeterConfigBuilderSuppplier.get()
                  .setTicksCounterSupplier(LongTicksCounter::new)
                  .setHl(2)
                  .build()),
          lockingStrategySupplier.get());
    }
  }

  @State(Scope.Group)
  public static class RateMeterContainer_GroupScope {
    ConcurrentNavigableMapRateMeter concurrentNavigableMapRateMeter;
    ConcurrentRingBufferRateMeter concurrentRingBufferRateMeter;
    ConcurrentSimpleRateMeter concurrentSimpleRateMeter;

    public RateMeterContainer_GroupScope() {
    }

    @Setup(Level.Trial)
    public final void setup() {
      concurrentNavigableMapRateMeter = new ConcurrentNavigableMapRateMeter(nanoTime(), samplesInterval,
          rateMeterConfigBuilderSuppplier.get()
              .setTicksCounterSupplier(LongAdderTicksCounter::new)
              .setHl(3)
              .build());
      final ConcurrentRingBufferRateMeterConfig.Builder concurrentRingBufferRateMeterConfigBuilder
          = ConcurrentRingBufferRateMeterConfig.newBuilder(rateMeterConfigBuilderSuppplier.get().build());
      concurrentRingBufferRateMeterConfigBuilder.setStrictTick(true)
          .setWaitStrategySupplier(waitStrategySupplier)
          .setLockStrategySupplier(lockingStrategySupplier)
          .setTicksCounterSupplier(LongAdderTicksCounter::new)
          .setHl(20);
      concurrentRingBufferRateMeter = new ConcurrentRingBufferRateMeter(nanoTime(), samplesInterval,
          concurrentRingBufferRateMeterConfigBuilder.build());
      concurrentSimpleRateMeter = new ConcurrentSimpleRateMeter(
          new RingBufferRateMeter(nanoTime(), samplesInterval,
              rateMeterConfigBuilderSuppplier.get()
                  .setTicksCounterSupplier(LongTicksCounter::new)
                  .setHl(2)
                  .build()),
          lockingStrategySupplier.get());
    }

    @TearDown(Level.Trial)
    public final void tearDown() {
      assertEquals(0, concurrentNavigableMapRateMeter.stats().failedAccuracyEventsCountForTick(), ACCEPTABLE_FAILED_ACCURACY_EVENTS_COUNT_PER_TRIAL);
      assertEquals(0, concurrentNavigableMapRateMeter.stats().failedAccuracyEventsCountForRate(), ACCEPTABLE_FAILED_ACCURACY_EVENTS_COUNT_PER_TRIAL);
      assertEquals(0, concurrentRingBufferRateMeter.stats().failedAccuracyEventsCountForTick(), ACCEPTABLE_FAILED_ACCURACY_EVENTS_COUNT_PER_TRIAL);
      assertEquals(0, concurrentRingBufferRateMeter.stats().failedAccuracyEventsCountForRate(), ACCEPTABLE_FAILED_ACCURACY_EVENTS_COUNT_PER_TRIAL);
      assertEquals(0, concurrentSimpleRateMeter.stats().failedAccuracyEventsCountForTick(), 0);
      assertEquals(0, concurrentSimpleRateMeter.stats().failedAccuracyEventsCountForRate(), ACCEPTABLE_FAILED_ACCURACY_EVENTS_COUNT_PER_TRIAL);
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