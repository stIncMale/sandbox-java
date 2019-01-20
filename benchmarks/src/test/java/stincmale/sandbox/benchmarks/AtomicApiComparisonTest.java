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
    return (long) BenchmarkState.varHandleLong.getAndAdd(state, 1L);
  }

  @Benchmark
  public final Long atomicReferenceLongGetAndIncrement(final BenchmarkState state) {
    return state.atomicReferenceLong.getAndAccumulate(1L, (v, acc) -> v + acc);
  }

  @Benchmark
  public final long atomicReferenceLongFieldUpdaterGetAndIncrement(final BenchmarkState state) {
    return BenchmarkState.atomicReferenceLongFieldUpdater.getAndAccumulate(state, 1L, (v, acc) -> v + acc);
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
  public final long atomicLongFieldUpdaterGetAndIncrementManual(final BenchmarkState state) {
    final AtomicLongFieldUpdater<BenchmarkState> atomicFieldUpdater = BenchmarkState.atomicLongFieldUpdater;
    long v;
    do {
      v = atomicFieldUpdater.get(state);
    } while (!atomicFieldUpdater.compareAndSet(state, v, v + 1L));
    return v;
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
  public final Long atomicReferenceLongGetAndIncrementManual(final BenchmarkState state) {
    final AtomicReference<Long> atomic = state.atomicReferenceLong;
    Long v;
    do {
      v = atomic.get();
    } while (!atomic.compareAndSet(v, v + 1L));
    return v;
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
