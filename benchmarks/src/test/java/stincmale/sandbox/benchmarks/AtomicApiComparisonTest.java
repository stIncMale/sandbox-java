package stincmale.sandbox.benchmarks;

import static java.lang.Math.min;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import static java.util.concurrent.locks.LockSupport.parkNanos;
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
import static stincmale.sandbox.benchmarks.util.JmhOptions.newOptionsBuilder;

/**
 * Test environment:
 * [single CPU] 3.4 GHz Intel Core i5 (4 cores),
 * [OS] macOS 10.13.6 (17G4015),
 * [JDK] OpenJDK 11.0.1+13 (<a href="https://jdk.java.net/11/">a build from Oracle</a>).
 * <pre>{@code
 * 1thread
 * Benchmark                                                                             Mode  Cnt    Score   Error   Units
 * AtomicApiComparisonTest.atomicLongGetAndIncrement                                    thrpt   45  139.645 ± 1.383  ops/us
 * AtomicApiComparisonTest.atomicLongGetAndIncrementManual                              thrpt   45  111.469 ± 0.665  ops/us
 * AtomicApiComparisonTest.atomicLongGetAndIncrementManualBackoff                       thrpt   45  111.545 ± 0.663  ops/us
 * AtomicApiComparisonTest.atomicLongFieldUpdaterGetAndIncrement                        thrpt   45  142.145 ± 0.142  ops/us
 * AtomicApiComparisonTest.atomicLongFieldUpdaterGetAndIncrementManual                  thrpt   45  108.689 ± 0.132  ops/us
 * AtomicApiComparisonTest.atomicLongFieldUpdaterGetAndIncrementManualBackoff           thrpt   45  108.789 ± 0.115  ops/us
 * AtomicApiComparisonTest.varHandleLongGetAndIncrement                                 thrpt   45  142.145 ± 0.156  ops/us
 * AtomicApiComparisonTest.varHandleLongGetAndIncrementManual                           thrpt   45  108.651 ± 0.099  ops/us
 * AtomicApiComparisonTest.varHandleLongGetAndIncrementManualBackoff                    thrpt   45  108.685 ± 0.145  ops/us
 *
 * AtomicApiComparisonTest.atomicReferenceLongGetAndIncrement                           thrpt   45   62.890 ± 6.959  ops/us
 * AtomicApiComparisonTest.atomicReferenceLongGetAndIncrementManual                     thrpt   45   60.380 ± 7.656  ops/us
 * AtomicApiComparisonTest.atomicReferenceLongGetAndIncrementManualBackoff              thrpt   45   56.293 ± 7.726  ops/us
 * AtomicApiComparisonTest.atomicReferenceLongFieldUpdaterGetAndIncrement               thrpt   45   66.397 ± 7.450  ops/us
 * AtomicApiComparisonTest.atomicReferenceLongFieldUpdaterGetAndIncrementManual         thrpt   45   66.904 ± 7.086  ops/us
 * AtomicApiComparisonTest.atomicReferenceLongFieldUpdaterGetAndIncrementManualBackoff  thrpt   45   64.524 ± 7.817  ops/us
 * AtomicApiComparisonTest.varHandleReferenceLongGetAndIncrement                                         impossible
 * AtomicApiComparisonTest.varHandleReferenceLongGetAndIncrementManual                  thrpt   45   43.501 ± 4.421  ops/us
 * AtomicApiComparisonTest.varHandleReferenceLongGetAndIncrementManualBackoff           thrpt   45   45.433 ± 4.770  ops/us
 *
 * AtomicApiComparisonTest.atomicLongCompareAndSet                                      thrpt   45  102.634 ± 0.132  ops/us
 * AtomicApiComparisonTest.atomicLongCompareAndSetBackoff                               thrpt   45  101.805 ± 0.229  ops/us
 * AtomicApiComparisonTest.atomicLongFieldUpdaterCompareAndSet                          thrpt   45  102.655 ± 0.099  ops/us
 * AtomicApiComparisonTest.atomicLongFieldUpdaterCompareAndSetBackoff                   thrpt   45  102.713 ± 0.091  ops/us
 * AtomicApiComparisonTest.varHandleLongCompareAndSet                                   thrpt   45  102.620 ± 0.099  ops/us
 * AtomicApiComparisonTest.varHandleLongCompareAndSetBackoff                            thrpt   45  102.588 ± 0.160  ops/us
 *
 * AtomicApiComparisonTest.atomicReferenceLongCompareAndSet                             thrpt   45   92.043 ± 0.166  ops/us
 * AtomicApiComparisonTest.atomicReferenceLongCompareAndSetBackoff                      thrpt   45   92.066 ± 0.085  ops/us
 * AtomicApiComparisonTest.atomicReferenceLongFieldUpdaterCompareAndSet                 thrpt   45  108.608 ± 0.135  ops/us
 * AtomicApiComparisonTest.atomicReferenceLongFieldUpdaterCompareAndSetBackoff          thrpt   45  108.576 ± 0.175  ops/us
 * AtomicApiComparisonTest.varHandleReferenceLongCompareAndSet                          thrpt   45  102.472 ± 0.119  ops/us
 * AtomicApiComparisonTest.varHandleReferenceLongCompareAndSetBackoff                   thrpt   45  102.381 ± 0.157  ops/us
 *
 * 4 threads
 * Benchmark                                                                             Mode  Cnt   Score   Error   Units
 * AtomicApiComparisonTest.atomicLongGetAndIncrement                                    thrpt   45  50.892 ± 0.131  ops/us
 * AtomicApiComparisonTest.atomicLongGetAndIncrementManual                              thrpt   45  12.435 ± 0.162  ops/us
 * AtomicApiComparisonTest.atomicLongGetAndIncrementManualBackoff                       thrpt   45  92.659 ± 1.459  ops/us
 * AtomicApiComparisonTest.atomicLongFieldUpdaterGetAndIncrement                        thrpt   45  50.844 ± 0.173  ops/us
 * AtomicApiComparisonTest.atomicLongFieldUpdaterGetAndIncrementManual                  thrpt   45  12.413 ± 0.179  ops/us
 * AtomicApiComparisonTest.atomicLongFieldUpdaterGetAndIncrementManualBackoff           thrpt   45  94.598 ± 1.746  ops/us
 * AtomicApiComparisonTest.varHandleLongGetAndIncrement                                 thrpt   45  50.801 ± 0.263  ops/us
 * AtomicApiComparisonTest.varHandleLongGetAndIncrementManual                           thrpt   45  12.448 ± 0.258  ops/us
 * AtomicApiComparisonTest.varHandleLongGetAndIncrementManualBackoff                    thrpt   45  95.568 ± 1.748  ops/us
 *
 * AtomicApiComparisonTest.atomicReferenceLongGetAndIncrement                           thrpt   45   9.895 ± 0.240  ops/us
 * AtomicApiComparisonTest.atomicReferenceLongGetAndIncrementManual                     thrpt   45  10.150 ± 0.279  ops/us
 * AtomicApiComparisonTest.atomicReferenceLongGetAndIncrementManualBackoff              thrpt   45  42.436 ± 3.994  ops/us
 * AtomicApiComparisonTest.atomicReferenceLongFieldUpdaterGetAndIncrement               thrpt   45   9.520 ± 0.379  ops/us
 * AtomicApiComparisonTest.atomicReferenceLongFieldUpdaterGetAndIncrementManual         thrpt   45  10.921 ± 0.270  ops/us
 * AtomicApiComparisonTest.atomicReferenceLongFieldUpdaterGetAndIncrementManualBackoff  thrpt   45  53.862 ± 6.298  ops/us
 * AtomicApiComparisonTest.varHandleReferenceLongGetAndIncrement                                        impossible
 * AtomicApiComparisonTest.varHandleReferenceLongGetAndIncrementManual                  thrpt   45   9.783 ± 0.459  ops/us
 * AtomicApiComparisonTest.varHandleReferenceLongGetAndIncrementManualBackoff           thrpt   45  31.106 ± 0.597  ops/us
 *
 * AtomicApiComparisonTest.atomicLongCompareAndSet                                      thrpt   45  11.936 ± 0.298  ops/us
 * AtomicApiComparisonTest.atomicLongCompareAndSetBackoff                               thrpt   45  86.616 ± 2.887  ops/us
 * AtomicApiComparisonTest.atomicLongFieldUpdaterCompareAndSet                          thrpt   45  12.345 ± 0.232  ops/us
 * AtomicApiComparisonTest.atomicLongFieldUpdaterCompareAndSetBackoff                   thrpt   45  87.988 ± 1.611  ops/us
 * AtomicApiComparisonTest.varHandleLongCompareAndSet                                   thrpt   45  12.007 ± 0.284  ops/us
 * AtomicApiComparisonTest.varHandleLongCompareAndSetBackoff                            thrpt   45  86.789 ± 1.235  ops/us
 *
 * AtomicApiComparisonTest.atomicReferenceLongCompareAndSet                             thrpt   45  11.998 ± 0.089  ops/us
 * AtomicApiComparisonTest.atomicReferenceLongCompareAndSetBackoff                      thrpt   45  78.966 ± 1.122  ops/us
 * AtomicApiComparisonTest.atomicReferenceLongFieldUpdaterCompareAndSet                 thrpt   45  12.142 ± 0.359  ops/us
 * AtomicApiComparisonTest.atomicReferenceLongFieldUpdaterCompareAndSetBackoff          thrpt   45  84.114 ± 2.184  ops/us
 * AtomicApiComparisonTest.varHandleReferenceLongCompareAndSet                          thrpt   45  12.478 ± 0.341  ops/us
 * AtomicApiComparisonTest.varHandleReferenceLongCompareAndSetBackoff                   thrpt   45  83.957 ± 1.341  ops/us
 *
 * 32 threads
 * Benchmark                                                                             Mode  Cnt    Score    Error   Units
 * AtomicApiComparisonTest.atomicLongGetAndIncrement                                    thrpt   45   51.844 ±  0.803  ops/us
 * AtomicApiComparisonTest.atomicLongGetAndIncrementManual                              thrpt   45   12.495 ±  0.331  ops/us
 * AtomicApiComparisonTest.atomicLongGetAndIncrementManualBackoff                       thrpt   45  157.771 ± 19.528  ops/us
 * AtomicApiComparisonTest.atomicLongFieldUpdaterGetAndIncrement                        thrpt   45   52.221 ±  0.908  ops/us
 * AtomicApiComparisonTest.atomicLongFieldUpdaterGetAndIncrementManual                  thrpt   45   12.448 ±  0.260  ops/us
 * AtomicApiComparisonTest.atomicLongFieldUpdaterGetAndIncrementManualBackoff           thrpt   45  184.738 ± 22.328  ops/us
 * AtomicApiComparisonTest.varHandleLongGetAndIncrement                                 thrpt   45   52.448 ±  0.810  ops/us
 * AtomicApiComparisonTest.varHandleLongGetAndIncrementManual                           thrpt   45   12.976 ±  1.617  ops/us
 * AtomicApiComparisonTest.varHandleLongGetAndIncrementManualBackoff                    thrpt   45  173.244 ± 19.481  ops/us
 *
 * AtomicApiComparisonTest.atomicReferenceLongGetAndIncrement                           thrpt   45   10.411 ±  0.278  ops/us
 * AtomicApiComparisonTest.atomicReferenceLongGetAndIncrementManual                     thrpt   45   10.480 ±  0.332  ops/us
 * AtomicApiComparisonTest.atomicReferenceLongGetAndIncrementManualBackoff              thrpt   45  112.475 ± 12.746  ops/us
 * AtomicApiComparisonTest.atomicReferenceLongFieldUpdaterGetAndIncrement               thrpt   45    9.844 ±  0.478  ops/us
 * AtomicApiComparisonTest.atomicReferenceLongFieldUpdaterGetAndIncrementManual         thrpt   45   11.100 ±  0.329  ops/us
 * AtomicApiComparisonTest.atomicReferenceLongFieldUpdaterGetAndIncrementManualBackoff  thrpt   45  121.222 ± 11.865  ops/us
 * AtomicApiComparisonTest.varHandleReferenceLongGetAndIncrement                                          impossible
 * AtomicApiComparisonTest.varHandleReferenceLongGetAndIncrementManual                  thrpt   45   10.342 ±  0.392  ops/us
 * AtomicApiComparisonTest.varHandleReferenceLongGetAndIncrementManualBackoff           thrpt   45   93.263 ±  8.888  ops/us
 *
 * AtomicApiComparisonTest.atomicLongCompareAndSet                                      thrpt   45   12.220 ±  0.250  ops/us
 * AtomicApiComparisonTest.atomicLongCompareAndSetBackoff                               thrpt   45  131.951 ±  8.388  ops/us
 * AtomicApiComparisonTest.atomicLongFieldUpdaterCompareAndSet                          thrpt   45   12.549 ±  0.286  ops/us
 * AtomicApiComparisonTest.atomicLongFieldUpdaterCompareAndSetBackoff                   thrpt   45  141.446 ± 12.780  ops/us
 * AtomicApiComparisonTest.varHandleLongCompareAndSet                                   thrpt   45   12.211 ±  0.219  ops/us
 * AtomicApiComparisonTest.varHandleLongCompareAndSetBackoff                            thrpt   45  136.625 ±  9.964  ops/us
 *
 * AtomicApiComparisonTest.atomicReferenceLongCompareAndSet                             thrpt   45   12.025 ±  0.192  ops/us
 * AtomicApiComparisonTest.atomicReferenceLongCompareAndSetBackoff                      thrpt   45  129.052 ±  8.041  ops/us
 * AtomicApiComparisonTest.atomicReferenceLongFieldUpdaterCompareAndSet                 thrpt   45   12.463 ±  0.245  ops/us
 * AtomicApiComparisonTest.atomicReferenceLongFieldUpdaterCompareAndSetBackoff          thrpt   45  114.061 ± 10.095  ops/us
 * AtomicApiComparisonTest.varHandleReferenceLongCompareAndSet                          thrpt   45   12.525 ±  0.278  ops/us
 * AtomicApiComparisonTest.varHandleReferenceLongCompareAndSetBackoff                   thrpt   45  125.939 ±  9.350  ops/us
 * }</pre>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AtomicApiComparisonTest {
  public AtomicApiComparisonTest() {
  }

  public final void runThroughputBenchmarks(final int numberOfThreads) throws RunnerException {
    new Runner(newOptionsBuilder(opts -> opts.forks(15)
        .warmupTime(milliseconds(200))
        .warmupIterations(10)
        .measurementTime(milliseconds(200))
        .measurementIterations(3))
        .include(includeClass(getClass()))
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
    private static final int DISTINCT_VALUES_PER_THREAD = 128;//2^7, must be a power of 2

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
      final int result = idx++;//get and increment
      idx = idx & (DISTINCT_VALUES_PER_THREAD - 1);//idx % DISTINCT_VALUES_PER_THREAD
      return result;
    }
  }

  private static final void backoff(final int numberOfFailures) {
    if (numberOfFailures > BenchmarkState.FAILURES_WITHOUT_BACKOFF) {
      parkNanos(min(numberOfFailures * BenchmarkState.EXPONENTIAL_BACKOFF_FACTOR, BenchmarkState.MAX_BACKOFF_NANOS));
    }
  }
}
