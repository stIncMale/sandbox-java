package stincmale.sandbox.benchmarks;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import static org.openjdk.jmh.runner.options.TimeValue.milliseconds;
import static stincmale.sandbox.benchmarks.util.JmhOptions.includeClass;
import static stincmale.sandbox.benchmarks.util.JmhOptions.jvmArgsDisableGc;
import static stincmale.sandbox.benchmarks.util.JmhOptions.newOptionsBuilder;

/**
 * Test environment:
 * [single CPU] 3.4 GHz Intel Core i5 (4 cores),
 * [OS] macOS 10.13.6 (17G4015),
 * [JDK] OpenJDK 11.0.1+13 (<a href="https://jdk.java.net/11/">a build from Oracle</a>).
 * <pre>{@code
 * Benchmark                                                                      Mode  Cnt   Score   Error   Units
 * AtomicApiComparisonTest.atomicLongGetAndIncrement                             thrpt   60  50.770 ± 0.142  ops/us
 * AtomicApiComparisonTest.atomicLongGetAndIncrementManual                       thrpt   60  12.728 ± 0.243  ops/us
 * AtomicApiComparisonTest.atomicLongFieldUpdaterGetAndIncrement                 thrpt   60  50.795 ± 0.119  ops/us
 * AtomicApiComparisonTest.atomicLongFieldUpdaterGetAndIncrementManual           thrpt   60  12.574 ± 0.122  ops/us
 * AtomicApiComparisonTest.varHandleLongGetAndIncrement                          thrpt   60  50.715 ± 0.169  ops/us
 * AtomicApiComparisonTest.varHandleLongGetAndIncrementManual                    thrpt   60  12.642 ± 0.243  ops/us
 *
 * AtomicApiComparisonTest.atomicReferenceLongGetAndIncrement                    thrpt   60  12.192 ± 0.488  ops/us
 * AtomicApiComparisonTest.atomicReferenceLongGetAndIncrementManual              thrpt   60  12.557 ± 0.615  ops/us
 * AtomicApiComparisonTest.atomicReferenceLongFieldUpdaterGetAndIncrement        thrpt   60  10.995 ± 0.405  ops/us
 * AtomicApiComparisonTest.atomicReferenceLongFieldUpdaterGetAndIncrementManual  thrpt   60  12.561 ± 0.322  ops/us
 * AtomicApiComparisonTest.varHandleReferenceLongGetAndIncrement                             impossible to do
 * AtomicApiComparisonTest.varHandleReferenceLongGetAndIncrementManual           thrpt   60  11.172 ± 0.538  ops/us
 *
 * AtomicApiComparisonTest.baselineLongCompareAndSet                             thrpt   60  171.222 ± 5.452  ops/us
 * AtomicApiComparisonTest.baselineReferenceLongCompareAndSet                    thrpt   60  106.773 ± 1.588  ops/us
 *
 * AtomicApiComparisonTest.atomicLongCompareAndSet                               thrpt   60  37.448 ± 0.079  ops/us
 * AtomicApiComparisonTest.atomicLongFieldUpdaterCompareAndSet                   thrpt   60  37.527 ± 0.191  ops/us
 * AtomicApiComparisonTest.varHandleLongCompareAndSet                            thrpt   60  37.511 ± 0.157  ops/us
 *
 * AtomicApiComparisonTest.atomicReferenceLongCompareAndSet                      thrpt   60  37.575 ± 0.268  ops/us
 * AtomicApiComparisonTest.atomicReferenceLongFieldUpdaterCompareAndSet          thrpt   60  37.061 ± 0.092  ops/us
 * AtomicApiComparisonTest.varHandleReferenceLongCompareAndSet                   thrpt   60  37.046 ± 0.117  ops/us
 * }</pre>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AtomicApiComparisonTest {
  public AtomicApiComparisonTest() {
  }

  @Test
  public final void run() throws RunnerException {
    new Runner(newOptionsBuilder(opts -> opts.forks(20)
        .warmupTime(milliseconds(100))
        .warmupIterations(10)
        .measurementTime(milliseconds(100))
        .measurementIterations(3))
        .include(includeClass(getClass()))
        .jvmArgsAppend(jvmArgsDisableGc())
        .mode(Mode.Throughput)
        .timeUnit(TimeUnit.MICROSECONDS)
        .threads(4)
        .build())
        .run();
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
  public final long varHandleLongGetAndIncrement(final BenchmarkState state) {
    return (long) BenchmarkState.varHandleLong.getAndAdd(state, 1L);
  }

  @Benchmark
  public final long varHandleLongGetAndIncrementManual(final BenchmarkState state) {
    final VarHandle varHandle = BenchmarkState.varHandleLong;
    long v;
    do {
      v = (long) varHandle.getVolatile(state);
    } while (!varHandle.compareAndSet(state, v, v + 1L));
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
  public final Long varHandleReferenceLongGetAndIncrementManual(final BenchmarkState state) {
    final VarHandle varHandle = BenchmarkState.varHandleReferenceLong;
    Long v;
    do {
      v = (Long) varHandle.getVolatile(state);
    } while (!varHandle.compareAndSet(state, v, v + 1L));
    return v;
  }

  @Benchmark
  public final long baselineLongCompareAndSet(final BenchmarkState state) {
    final long next = BenchmarkState.getNextPingOrPong(state.plainLong);
    state.plainLong = next;
    return next;
  }

  @Benchmark
  public final Long baselineReferenceLongCompareAndSet(final BenchmarkState state) {
    final Long next = BenchmarkState.getNextPingOrPong(state.plainReferenceLong);
    state.plainReferenceLong = next;
    return next;
  }

  @Benchmark
  public final boolean atomicLongCompareAndSet(final BenchmarkState state) {
    final AtomicLong atomic = state.atomicLong;
    final long expected = atomic.get();
    return atomic.compareAndSet(expected, BenchmarkState.getNextPingOrPong(expected));
  }

  @Benchmark
  public final boolean atomicLongFieldUpdaterCompareAndSet(final BenchmarkState state) {
    final AtomicLongFieldUpdater<BenchmarkState> atomicFieldUpdater = BenchmarkState.atomicLongFieldUpdater;
    final long expected = atomicFieldUpdater.get(state);
    return atomicFieldUpdater.compareAndSet(state, expected, BenchmarkState.getNextPingOrPong(expected));
  }

  @Benchmark
  public final boolean varHandleLongCompareAndSet(final BenchmarkState state) {
    final VarHandle varHandle = BenchmarkState.varHandleLong;
    final long expected = (long) varHandle.getVolatile(state);
    return varHandle.compareAndSet(state, expected, BenchmarkState.getNextPingOrPong(expected));
  }

  @Benchmark
  public final boolean atomicReferenceLongCompareAndSet(final BenchmarkState state) {
    final AtomicReference<Long> atomic = state.atomicReferenceLong;
    final Long expected = atomic.get();
    return atomic.compareAndSet(expected, BenchmarkState.getNextPingOrPong(expected));
  }

  @Benchmark
  public final boolean atomicReferenceLongFieldUpdaterCompareAndSet(final BenchmarkState state) {
    final AtomicReferenceFieldUpdater<BenchmarkState, Long> atomicFieldUpdater = BenchmarkState.atomicReferenceLongFieldUpdater;
    final Long expected = atomicFieldUpdater.get(state);
    return atomicFieldUpdater.compareAndSet(state, expected, BenchmarkState.getNextPingOrPong(expected));
  }

  @Benchmark
  public final boolean varHandleReferenceLongCompareAndSet(final BenchmarkState state) {
    final VarHandle varHandle = BenchmarkState.varHandleReferenceLong;
    final Long expected = (Long) varHandle.getVolatile(state);
    return varHandle.compareAndSet(state, expected, BenchmarkState.getNextPingOrPong(expected));
  }

  @State(Scope.Benchmark)
  public static class BenchmarkState {
    private static final long pingLong;
    private static final long pongLong;
    private static final Long pingReferenceLong;
    private static final Long pongReferenceLong;
    private static final AtomicLongFieldUpdater<BenchmarkState> atomicLongFieldUpdater;
    private static final AtomicReferenceFieldUpdater<BenchmarkState, Long> atomicReferenceLongFieldUpdater;
    private static final VarHandle varHandleLong;
    private static final VarHandle varHandleReferenceLong;

    static {
      pingLong = 314159L;
      pongLong = -pingLong;
      pingReferenceLong = pingLong;
      pongReferenceLong = pongLong;
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
    private long plainLong;
    private Long plainReferenceLong;

    public BenchmarkState() {
    }

    @Setup(Level.Iteration)
    public final void setup() {
      atomicLong = new AtomicLong(pingLong);
      atomicReferenceLong = new AtomicReference<>(pingReferenceLong);
      atomicLongFieldUpdater.set(this, pingLong);
      atomicReferenceLongFieldUpdater.set(this, pingReferenceLong);
      varHandleLong.setVolatile(this, pingLong);
      varHandleReferenceLong.setVolatile(this, pingReferenceLong);
      plainLong = pingLong;
      plainReferenceLong = pingReferenceLong;
    }

    private static final long getNextPingOrPong(final long pingOrPong) {
      return pingOrPong == pingLong ? pongLong : pingLong;
    }

    private static final Long getNextPingOrPong(final Long pingOrPong) {
      return pingOrPong.equals(pingReferenceLong) ? pongReferenceLong : pingReferenceLong;
    }
  }
}
