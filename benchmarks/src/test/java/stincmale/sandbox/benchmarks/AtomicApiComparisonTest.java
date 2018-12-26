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
import stincmale.sandbox.benchmarks.util.JmhOptions;

/**
 * <pre>{@code
 * 1 thread
 * Benchmark                                                              Mode  Cnt    Score   Error   Units
 * AtomicApiComparisonTest.atomicLongCompareAndSet                       thrpt   80  110.148 ± 0.211  ops/us
 * AtomicApiComparisonTest.atomicLongFieldUpdaterCompareAndSet           thrpt   80  106.919 ± 0.237  ops/us
 * AtomicApiComparisonTest.varHandleLongCompareAndSet                    thrpt   80  106.765 ± 0.261  ops/us
 *
 * AtomicApiComparisonTest.atomicReferenceLongCompareAndSet              thrpt   80   88.614 ± 0.167  ops/us
 * AtomicApiComparisonTest.atomicReferenceLongFieldUpdaterCompareAndSet  thrpt   80   90.730 ± 0.239  ops/us
 * AtomicApiComparisonTest.varHandleReferenceLongCompareAndSet           thrpt   80   90.823 ± 0.220  ops/us
 *
 * AtomicApiComparisonTest.atomicLongGetAndIncrement                     thrpt   80  139.912 ± 0.273  ops/us
 * AtomicApiComparisonTest.atomicLongFieldUpdaterGetAndIncrement         thrpt   80  139.881 ± 0.275  ops/us
 * AtomicApiComparisonTest.varHandleLongGetAndIncrement                  thrpt   80  106.625 ± 0.385  ops/us
 *
 * 4 threads
 * Benchmark                                                              Mode  Cnt   Score   Error   Units
 * AtomicApiComparisonTest.atomicLongCompareAndSet                       thrpt   80  37.839 ± 0.437  ops/us
 * AtomicApiComparisonTest.atomicLongFieldUpdaterCompareAndSet           thrpt   80  37.350 ± 0.074  ops/us
 * AtomicApiComparisonTest.varHandleLongCompareAndSet                    thrpt   80  37.465 ± 0.109  ops/us
 *
 * AtomicApiComparisonTest.atomicReferenceLongCompareAndSet              thrpt   80  37.550 ± 0.079  ops/us
 * AtomicApiComparisonTest.atomicReferenceLongFieldUpdaterCompareAndSet  thrpt   80  36.902 ± 0.082  ops/us
 * AtomicApiComparisonTest.varHandleReferenceLongCompareAndSet           thrpt   80  36.831 ± 0.114  ops/us
 *
 * AtomicApiComparisonTest.atomicLongGetAndIncrement                     thrpt   80  50.571 ± 0.140  ops/us
 * AtomicApiComparisonTest.atomicLongFieldUpdaterGetAndIncrement         thrpt   80  50.907 ± 0.111  ops/us
 * AtomicApiComparisonTest.varHandleLongGetAndIncrement                  thrpt   80  13.187 ± 0.274  ops/us
 *
 * 32 threads
 * Benchmark                                                              Mode  Cnt   Score   Error   Units
 * AtomicApiComparisonTest.atomicLongCompareAndSet                       thrpt   80  39.057 ± 0.696  ops/us
 * AtomicApiComparisonTest.atomicLongFieldUpdaterCompareAndSet           thrpt   80  39.873 ± 3.347  ops/us
 * AtomicApiComparisonTest.varHandleLongCompareAndSet                    thrpt   80  38.429 ± 0.557  ops/us
 *
 * AtomicApiComparisonTest.atomicReferenceLongCompareAndSet              thrpt   80  38.418 ± 0.516  ops/us
 * AtomicApiComparisonTest.atomicReferenceLongFieldUpdaterCompareAndSet  thrpt   80  37.712 ± 0.439  ops/us
 * AtomicApiComparisonTest.varHandleReferenceLongCompareAndSet           thrpt   80  37.873 ± 0.665  ops/us
 *
 * AtomicApiComparisonTest.atomicLongGetAndIncrement                     thrpt   80  52.157 ± 0.752  ops/us
 * AtomicApiComparisonTest.atomicLongFieldUpdaterGetAndIncrement         thrpt   80  52.212 ± 0.904  ops/us
 * AtomicApiComparisonTest.varHandleLongGetAndIncrement                  thrpt   80  13.091 ± 0.366  ops/us
 * }</pre>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AtomicApiComparisonTest {
  public AtomicApiComparisonTest() {
  }

  private static final void runThroughputBenchmarks(final int numberOfThreads) throws RunnerException {
    new Runner(
        JmhOptions.includingClass(AtomicApiComparisonTest.class)
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

  @Benchmark
  public final long atomicLongGetAndIncrement(final BenchmarkState state) {
    return state.atomicLong.getAndIncrement();
  }

  @Benchmark
  public final long atomicLongFieldUpdaterGetAndIncrement(final BenchmarkState state) {
    return BenchmarkState.atomicLongFieldUpdater.getAndIncrement(state);
  }

  @Benchmark
  public final long varHandleLongGetAndIncrement(final BenchmarkState state) {
    return getAndIncrement(BenchmarkState.varHandleLong, state);
  }

  private static final long getAndIncrement(final VarHandle varHandle, final Object obj) {
    long v;
    do {
      v = (long) varHandle.getVolatile(obj);
    } while (!varHandle.compareAndSet(obj, v, v + 1));
    return v;
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
      pingLong = 314159;
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

    public BenchmarkState() {
    }

    @Setup(Level.Trial)
    public final void setup() {
      atomicLong = new AtomicLong(pingLong);
      atomicReferenceLong = new AtomicReference<>(pingReferenceLong);
      atomicLongFieldUpdater.set(this, pingLong);
      atomicReferenceLongFieldUpdater.set(this, pingReferenceLong);
      varHandleLong.setVolatile(this, pingLong);
      varHandleReferenceLong.setVolatile(this, pingReferenceLong);
    }

    private static final long getNextPingOrPong(final long pingOrPong) {
      return pingOrPong == pingLong ? pongLong : pingLong;
    }

    private static final Long getNextPingOrPong(final Long pingOrPong) {
      return pingOrPong.equals(pingReferenceLong) ? pongReferenceLong : pingReferenceLong;
    }
  }
}
