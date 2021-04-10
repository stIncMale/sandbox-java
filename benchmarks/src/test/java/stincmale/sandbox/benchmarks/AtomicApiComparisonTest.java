package stincmale.sandbox.benchmarks;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import static java.lang.Math.min;
import static java.util.concurrent.locks.LockSupport.parkNanos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import static org.openjdk.jmh.runner.options.TimeValue.milliseconds;
import static stincmale.sandbox.benchmarks.util.JmhOptions.includeBenchmarks;
import static stincmale.sandbox.benchmarks.util.JmhOptions.newOptionsBuilder;

@TestInstance(Lifecycle.PER_CLASS)
public class AtomicApiComparisonTest {
  public AtomicApiComparisonTest() {
  }

  public final void runThroughputBenchmarks(final int numberOfThreads) throws RunnerException {
    new Runner(newOptionsBuilder(opts -> opts.forks(15)
        .warmupTime(milliseconds(200))
        .warmupIterations(10)
        .measurementTime(milliseconds(200))
        .measurementIterations(3))
        .include(includeBenchmarks(getClass()))
        .mode(Mode.Throughput)
        .timeUnit(TimeUnit.MICROSECONDS)
        .threads(numberOfThreads)
        .build())
        .run();
  }

  @Test
  public final void throughputThreads1() throws RunnerException {
    runThroughputBenchmarks(1);
  }

  @Test
  public final void throughputThreads4() throws RunnerException {
    runThroughputBenchmarks(4);
  }

  @Test
  public final void throughputThreads32() throws RunnerException {
    runThroughputBenchmarks(32);
  }

  @Benchmark
  public final long atomicLongGetAndIncrement(final BenchmarkState state) {
    return state.atomicLong.getAndIncrement();
  }

  @Benchmark
  public final long atomicLongGetAndIncrementManual(final BenchmarkState state) {
    final AtomicLong atomic = state.atomicLong;
    long v;
    do {
      v = atomic.get();
    } while (!atomic.compareAndSet(v, v + 1L));
    return v;
  }

  @Benchmark
  public final long atomicLongGetAndIncrementManualBackoff(final BenchmarkState state) {
    final AtomicLong atomic = state.atomicLong;
    long v;
    for (int numberOfFailures = 1; ; numberOfFailures++) {
      v = atomic.get();
      if (atomic.compareAndSet(v, v + 1L)) {
        break;
      }
      backoff(numberOfFailures);
    }
    return v;
  }

  @Benchmark
  public final long atomicLongFieldUpdaterGetAndIncrement(final BenchmarkState state) {
    return BenchmarkState.atomicLongFieldUpdater.getAndIncrement(state);
  }

  @Benchmark
  public final long atomicLongFieldUpdaterGetAndIncrementManual(final BenchmarkState state) {
    final AtomicLongFieldUpdater<BenchmarkState> atomicFieldUpdater = BenchmarkState.atomicLongFieldUpdater;
    long v;
    do {
      v = atomicFieldUpdater.get(state);
    } while (!atomicFieldUpdater.compareAndSet(state, v, v + 1L));
    return v;
  }

  @Benchmark
  public final long atomicLongFieldUpdaterGetAndIncrementManualBackoff(final BenchmarkState state) {
    final AtomicLongFieldUpdater<BenchmarkState> atomicFieldUpdater = BenchmarkState.atomicLongFieldUpdater;
    long v;
    for (int numberOfFailures = 1; ; numberOfFailures++) {
      v = atomicFieldUpdater.get(state);
      if (atomicFieldUpdater.compareAndSet(state, v, v + 1L)) {
        break;
      }
      backoff(numberOfFailures);
    }
    return v;
  }

  @Benchmark
  public final long varHandleLongGetAndIncrement(final BenchmarkState state) {
    return (long)BenchmarkState.varHandleLong.getAndAdd(state, 1L);
  }

  @Benchmark
  public final long varHandleLongGetAndIncrementManual(final BenchmarkState state) {
    final VarHandle varHandle = BenchmarkState.varHandleLong;
    long v;
    do {
      v = (long)varHandle.getVolatile(state);
    } while (!varHandle.compareAndSet(state, v, v + 1L));
    return v;
  }

  @Benchmark
  public final long varHandleLongGetAndIncrementManualBackoff(final BenchmarkState state) {
    final VarHandle varHandle = BenchmarkState.varHandleLong;
    long v;
    for (int numberOfFailures = 1; ; numberOfFailures++) {
      v = (long)varHandle.getVolatile(state);
      if (varHandle.compareAndSet(state, v, v + 1L)) {
        break;
      }
      backoff(numberOfFailures);
    }
    return v;
  }

  @Benchmark
  public final Long atomicReferenceLongGetAndIncrement(final BenchmarkState state) {
    return state.atomicReferenceLong.getAndAccumulate(1L, (v, acc) -> v + acc);
  }

  @Benchmark
  public final Long atomicReferenceLongGetAndIncrementManual(final BenchmarkState state) {
    final AtomicReference<Long> atomic = state.atomicReferenceLong;
    Long v;
    do {
      v = atomic.get();
    } while (!atomic.compareAndSet(v, v + 1L));
    return v;
  }

  @Benchmark
  public final Long atomicReferenceLongGetAndIncrementManualBackoff(final BenchmarkState state) {
    final AtomicReference<Long> atomic = state.atomicReferenceLong;
    Long v;
    for (int numberOfFailures = 1; ; numberOfFailures++) {
      v = atomic.get();
      if (atomic.compareAndSet(v, v + 1L)) {
        break;
      }
      backoff(numberOfFailures);
    }
    return v;
  }

  @Benchmark
  public final long atomicReferenceLongFieldUpdaterGetAndIncrement(final BenchmarkState state) {
    return BenchmarkState.atomicReferenceLongFieldUpdater.getAndAccumulate(state, 1L, (v, acc) -> v + acc);
  }

  @Benchmark
  public final Long atomicReferenceLongFieldUpdaterGetAndIncrementManual(final BenchmarkState state) {
    final AtomicReferenceFieldUpdater<BenchmarkState, Long> atomicFieldUpdater = BenchmarkState.atomicReferenceLongFieldUpdater;
    Long v;
    do {
      v = atomicFieldUpdater.get(state);
    } while (!atomicFieldUpdater.compareAndSet(state, v, v + 1L));
    return v;
  }

  @Benchmark
  public final Long atomicReferenceLongFieldUpdaterGetAndIncrementManualBackoff(final BenchmarkState state) {
    final AtomicReferenceFieldUpdater<BenchmarkState, Long> atomicFieldUpdater = BenchmarkState.atomicReferenceLongFieldUpdater;
    Long v;
    for (int numberOfFailures = 1; ; numberOfFailures++) {
      v = atomicFieldUpdater.get(state);
      if (atomicFieldUpdater.compareAndSet(state, v, v + 1L)) {
        break;
      }
      backoff(numberOfFailures);
    }
    return v;
  }

  @Benchmark
  public final Long varHandleReferenceLongGetAndIncrementManual(final BenchmarkState state) {
    final VarHandle varHandle = BenchmarkState.varHandleReferenceLong;
    Long v;
    do {
      v = (Long)varHandle.getVolatile(state);
    } while (!varHandle.compareAndSet(state, v, v + 1L));
    return v;
  }

  @Benchmark
  public final Long varHandleReferenceLongGetAndIncrementManualBackoff(final BenchmarkState state) {
    final VarHandle varHandle = BenchmarkState.varHandleReferenceLong;
    Long v;
    for (int numberOfFailures = 1; ; numberOfFailures++) {
      v = (Long)varHandle.getVolatile(state);
      if (varHandle.compareAndSet(state, v, v + 1L)) {
        break;
      }
      backoff(numberOfFailures);
    }
    return v;
  }

  @Benchmark
  public final long atomicLongCompareAndSet(final BenchmarkState benchmarkState, final ThreadState threadState) {
    final long newV = threadState.getAndAdvanceLong();
    final AtomicLong atomic = benchmarkState.atomicLong;
    long v;
    do {
      v = atomic.get();
    } while (!atomic.compareAndSet(v, newV));
    return v;
  }

  @Benchmark
  public final long atomicLongCompareAndSetBackoff(final BenchmarkState benchmarkState, final ThreadState threadState) {
    final long newV = threadState.getAndAdvanceLong();
    final AtomicLong atomic = benchmarkState.atomicLong;
    long v;
    for (int numberOfFailures = 1; ; numberOfFailures++) {
      v = atomic.get();
      if (atomic.compareAndSet(v, newV)) {
        break;
      }
      backoff(numberOfFailures);
    }
    return v;
  }

  @Benchmark
  public final long atomicLongFieldUpdaterCompareAndSet(final BenchmarkState benchmarkState, final ThreadState threadState) {
    final long newV = threadState.getAndAdvanceLong();
    final AtomicLongFieldUpdater<BenchmarkState> atomicFieldUpdater = BenchmarkState.atomicLongFieldUpdater;
    long v;
    do {
      v = atomicFieldUpdater.get(benchmarkState);
    } while (!atomicFieldUpdater.compareAndSet(benchmarkState, v, newV));
    return v;
  }

  @Benchmark
  public final long atomicLongFieldUpdaterCompareAndSetBackoff(final BenchmarkState benchmarkState, final ThreadState threadState) {
    final long newV = threadState.getAndAdvanceLong();
    final AtomicLongFieldUpdater<BenchmarkState> atomicFieldUpdater = BenchmarkState.atomicLongFieldUpdater;
    long v;
    for (int numberOfFailures = 1; ; numberOfFailures++) {
      v = atomicFieldUpdater.get(benchmarkState);
      if (atomicFieldUpdater.compareAndSet(benchmarkState, v, newV)) {
        break;
      }
      backoff(numberOfFailures);
    }
    return v;
  }

  @Benchmark
  public final long varHandleLongCompareAndSet(final BenchmarkState benchmarkState, final ThreadState threadState) {
    final long newV = threadState.getAndAdvanceLong();
    final VarHandle varHandle = BenchmarkState.varHandleLong;
    long v;
    do {
      v = (long)varHandle.get(benchmarkState);
    } while (!varHandle.compareAndSet(benchmarkState, v, newV));
    return v;
  }

  @Benchmark
  public final long varHandleLongCompareAndSetBackoff(final BenchmarkState benchmarkState, final ThreadState threadState) {
    final long newV = threadState.getAndAdvanceLong();
    final VarHandle varHandle = BenchmarkState.varHandleLong;
    long v;
    for (int numberOfFailures = 1; ; numberOfFailures++) {
      v = (long)varHandle.get(benchmarkState);
      if (varHandle.compareAndSet(benchmarkState, v, newV)) {
        break;
      }
      backoff(numberOfFailures);
    }
    return v;
  }

  @Benchmark
  public final Long atomicReferenceLongCompareAndSet(final BenchmarkState benchmarkState, final ThreadState threadState) {
    final Long newV = threadState.getAndAdvanceReferenceLong();
    final AtomicReference<Long> atomic = benchmarkState.atomicReferenceLong;
    Long v;
    do {
      v = atomic.get();
    } while (!atomic.compareAndSet(v, newV));
    return v;
  }

  @Benchmark
  public final Long atomicReferenceLongCompareAndSetBackoff(final BenchmarkState benchmarkState, final ThreadState threadState) {
    final Long newV = threadState.getAndAdvanceReferenceLong();
    final AtomicReference<Long> atomic = benchmarkState.atomicReferenceLong;
    Long v;
    for (int numberOfFailures = 1; ; numberOfFailures++) {
      v = atomic.get();
      if (atomic.compareAndSet(v, newV)) {
        break;
      }
      backoff(numberOfFailures);
    }
    return v;
  }

  @Benchmark
  public final Long atomicReferenceLongFieldUpdaterCompareAndSet(final BenchmarkState benchmarkState, final ThreadState threadState) {
    final Long newV = threadState.getAndAdvanceReferenceLong();
    final AtomicReferenceFieldUpdater<BenchmarkState, Long> atomicFieldUpdater = BenchmarkState.atomicReferenceLongFieldUpdater;
    Long v;
    do {
      v = atomicFieldUpdater.get(benchmarkState);
    } while (!atomicFieldUpdater.compareAndSet(benchmarkState, v, newV));
    return v;
  }

  @Benchmark
  public final Long atomicReferenceLongFieldUpdaterCompareAndSetBackoff(final BenchmarkState benchmarkState, final ThreadState threadState) {
    final Long newV = threadState.getAndAdvanceReferenceLong();
    final AtomicReferenceFieldUpdater<BenchmarkState, Long> atomicFieldUpdater = BenchmarkState.atomicReferenceLongFieldUpdater;
    Long v;
    for (int numberOfFailures = 1; ; numberOfFailures++) {
      v = atomicFieldUpdater.get(benchmarkState);
      if (atomicFieldUpdater.compareAndSet(benchmarkState, v, newV)) {
        break;
      }
      backoff(numberOfFailures);
    }
    return v;
  }

  @Benchmark
  public final Long varHandleReferenceLongCompareAndSet(final BenchmarkState benchmarkState, final ThreadState threadState) {
    final Long newV = threadState.getAndAdvanceReferenceLong();
    final VarHandle varHandle = BenchmarkState.varHandleReferenceLong;
    Long v;
    do {
      v = (Long)varHandle.getVolatile(benchmarkState);
    } while (!varHandle.compareAndSet(benchmarkState, v, newV));
    return v;
  }

  @Benchmark
  public final Long varHandleReferenceLongCompareAndSetBackoff(final BenchmarkState benchmarkState, final ThreadState threadState) {
    final Long newV = threadState.getAndAdvanceReferenceLong();
    final VarHandle varHandle = BenchmarkState.varHandleReferenceLong;
    Long v;
    for (int numberOfFailures = 1; ; numberOfFailures++) {
      v = (Long)varHandle.getVolatile(benchmarkState);
      if (varHandle.compareAndSet(benchmarkState, v, newV)) {
        break;
      }
      backoff(numberOfFailures);
    }
    return v;
  }

  @State(Scope.Benchmark)
  public static class BenchmarkState {
    private static final int FAILURES_WITHOUT_BACKOFF = 0;
    private static final int EXPONENTIAL_BACKOFF_FACTOR = 2;
    private static final int MAX_BACKOFF_NANOS = 10;
    private static final AtomicLongFieldUpdater<BenchmarkState> atomicLongFieldUpdater;
    private static final AtomicReferenceFieldUpdater<BenchmarkState, Long> atomicReferenceLongFieldUpdater;
    private static final VarHandle varHandleLong;
    private static final VarHandle varHandleReferenceLong;

    static {
      atomicLongFieldUpdater = AtomicLongFieldUpdater.newUpdater(BenchmarkState.class, "forAtomicLongFieldUpdater");
      atomicReferenceLongFieldUpdater = AtomicReferenceFieldUpdater.newUpdater(
          BenchmarkState.class, Long.class, "forAtomicReferenceLongFieldUpdater");
      try {
        varHandleLong = MethodHandles.lookup().findVarHandle(BenchmarkState.class, "forVarHandleLong", long.class);
        varHandleReferenceLong = MethodHandles.lookup().findVarHandle(BenchmarkState.class, "forVarHandleReferenceLong", Long.class);
      } catch (final NoSuchFieldException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }

    private AtomicLong atomicLong;
    private AtomicReference<Long> atomicReferenceLong;
    private volatile long forAtomicLongFieldUpdater;
    private volatile Long forAtomicReferenceLongFieldUpdater;
    private volatile long forVarHandleLong;
    private volatile Long forVarHandleReferenceLong;

    public BenchmarkState() {
    }

    @Setup(Level.Trial)
    public final void setup() {
      atomicLong = new AtomicLong();
      atomicReferenceLong = new AtomicReference<>(0L);
      atomicLongFieldUpdater.set(this, 0L);
      atomicReferenceLongFieldUpdater.set(this, 0L);
      varHandleLong.setVolatile(this, 0L);
      varHandleReferenceLong.setVolatile(this, 0L);
    }
  }

  @State(Scope.Thread)
  public static class ThreadState {
    private static final int DISTINCT_VALUES_PER_THREAD = 128;// 2^7, must be a power of 2

    private long[] longs;//all values are distinct for a given thread, but some may be equal (highly unlikely) between threads
    private Long[] referenceLongs;
    private int idx;

    public ThreadState() {
    }

    @Setup(Level.Trial)
    public final void setup() {
      longs = ThreadLocalRandom.current()
          .longs()
          .distinct()
          .limit(DISTINCT_VALUES_PER_THREAD)
          .toArray();
      referenceLongs = Arrays.stream(longs)
          .boxed()
          .toArray(Long[]::new);
      idx = 0;
    }

    private final long getAndAdvanceLong() {
      return longs[getAndIncrementIdx()];
    }

    private Long getAndAdvanceReferenceLong() {
      return referenceLongs[getAndIncrementIdx()];
    }

    private int getAndIncrementIdx() {
      final int result = idx++;// get and increment
      idx = idx & (DISTINCT_VALUES_PER_THREAD - 1);// idx % DISTINCT_VALUES_PER_THREAD
      return result;
    }
  }

  private static final void backoff(final int numberOfFailures) {
    if (numberOfFailures > BenchmarkState.FAILURES_WITHOUT_BACKOFF) {
      parkNanos(min(numberOfFailures * BenchmarkState.EXPONENTIAL_BACKOFF_FACTOR, BenchmarkState.MAX_BACKOFF_NANOS));
    }
  }
}
