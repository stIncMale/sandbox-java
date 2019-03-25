package stincmale.sandbox.benchmarks;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.StampedLock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import static org.openjdk.jmh.runner.options.TimeValue.milliseconds;
import static stincmale.sandbox.benchmarks.util.JmhOptions.includeBenchmarks;

/**
 * Test environment:
 * [single CPU] 3.4 GHz Intel Core i5 (4 cores, 4 hardware threads),
 * [OS] macOS 10.13.6 (17G4015),
 * [JDK] OpenJDK 11.0.1+13 (<a href="https://jdk.java.net/11/">a build from Oracle</a>).
 * <pre>{@code
 * 4 threads
 * Benchmark                 Mode  Cnt   Score    Error   Units
 * TmpTest.reentrantRwLock  thrpt    4  45.029 ± 35.699  ops/ms
 * TmpTest.stampedLock      thrpt    4  39.478 ± 55.836  ops/ms
 *
 * 32 threads
 * Benchmark                 Mode  Cnt     Score       Error   Units
 * TmpTest.reentrantRwLock  thrpt    4    35.167 ±    31.066  ops/ms
 * TmpTest.stampedLock      thrpt    4  2298.800 ± 19728.618  ops/ms
 *
 *
 * # JMH version: 1.21
 * # VM version: JDK 11.0.2, OpenJDK 64-Bit Server VM, 11.0.2+9
 * # VM invoker: /Library/Java/JavaVirtualMachines/jdk-11.0.2.jdk/Contents/Home/bin/java
 * # VM options: -Xfuture --illegal-access=deny -Xshare:off -Xms1024m -Xmx1024m -server -disableassertions
 * # Warmup: 6 iterations, 1000 ms each
 * # Measurement: 2 iterations, 1000 ms each
 * # Timeout: 1000000 ms per iteration
 * # Threads: 32 threads, will synchronize iterations
 * # Benchmark mode: Throughput, ops/time
 * # Benchmark: stincmale.sandbox.benchmarks.TmpTest.stampedLock
 *
 * # Run progress: 50.00% complete, ETA 00:00:25
 * # Fork: 1 of 2
 * # Warmup Iteration   1: 14.844 ops/ms
 * # Warmup Iteration   2: 40.329 ops/ms
 * # Warmup Iteration   3: 44.688 ops/ms
 * # Warmup Iteration   4: 28.657 ops/ms
 * # Warmup Iteration   5: 18.564 ops/ms
 * # Warmup Iteration   6: 19.878 ops/ms
 * Iteration   1: 955.863 ops/ms
 * Iteration   2: 6793.449 ops/ms
 *
 * # Run progress: 75.00% complete, ETA 00:00:25
 * # Fork: 2 of 2
 * # Warmup Iteration   1: 3.991 ops/ms
 * # Warmup Iteration   2: 509.107 ops/ms
 * # Warmup Iteration   3: 26.467 ops/ms
 * # Warmup Iteration   4: 22.406 ops/ms
 * # Warmup Iteration   5: 33.943 ops/ms
 * # Warmup Iteration   6: 3147.069 ops/ms
 * Iteration   1: 19.063 ops/ms
 * Iteration   2: 1426.824 ops/ms
 * }</pre>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TmpTest {
  public TmpTest() {
  }

  public final void runThroughputBenchmarks(final int numberOfThreads) throws RunnerException {
    final OptionsBuilder opts = new OptionsBuilder();
    opts.jvmArgs("-Xfuture", "--illegal-access=deny", "-Xshare:off", "-Xms1024m", "-Xmx1024m", "-server", "-disableassertions")
        .shouldDoGC(false)
        .syncIterations(true)
        .shouldFailOnError(true)
        .threads(1)
        .timeout(milliseconds(1000_000))
        .forks(2)
        .warmupTime(milliseconds(1000))
        .warmupIterations(0)
        .measurementTime(milliseconds(1000))
        .measurementIterations(5)
        .include(includeBenchmarks(TmpTest.class))
        .shouldDoGC(false)
        .mode(Mode.Throughput)
        .timeUnit(TimeUnit.SECONDS)
        .threads(numberOfThreads);
    new Runner(opts.build()).run();
  }

  @Test
  public final void throughputThreads4() throws RunnerException {
    runThroughputBenchmarks(4);
  }

  @Test
  public final void throughputThreads32() throws RunnerException {
    runThroughputBenchmarks(32);
  }

  //  @Benchmark
  //  public final long reentrantRwLock(final BenchmarkState s) throws InterruptedException {
  //    s.rwLock.readLock().stampedLock();
  //    try {
  //      if (s.counter.getAndIncrement() < 1000) {
  //        return s.counter.get();
  //      }
  //    } finally {
  //      s.rwLock.readLock().unlock();
  //    }
  //    s.rwLock.writeLock().stampedLock();
  //    try {
  //      Thread.sleep(10);
  //      s.counter.set(0);
  //    } finally {
  //      s.rwLock.writeLock().unlock();
  //    }
  //    return s.counter.get();
  //  }

  @Benchmark
  public final long stampedLock(final BenchmarkState state) throws InterruptedException {
    long counter;
    boolean writeLock = false;
    long stamp = state.stampedLock.readLock();
    try {
      for (; ; writeLock = true, stamp = state.stampedLock.writeLock()) {
        counter = state.counter.incrementAndGet();
        if (counter < 10) {//counter is fine
          break;
        } else {//counter is too big, we may need to reset counter
          if (writeLock) {
            //            System.out.println(String.format("tid=%s, counter=%s", Thread.currentThread().getId(), counter));
            counter = 0;
            state.counter.set(counter);
            Thread.sleep(100);
            break;
          } else {
            state.stampedLock.unlockRead(stamp);
          }
        }
      }
    } finally {
      if (writeLock) {
        state.stampedLock.unlockWrite(stamp);
      } else {
        state.stampedLock.unlockRead(stamp);
      }
    }
    state.iterationCounter.getAndIncrement();
    return counter;
  }

  @State(Scope.Benchmark)
  public static class BenchmarkState {
    private StampedLock stampedLock;
    private ReentrantReadWriteLock rwLock;
    private AtomicLong counter;
    private AtomicLong iterationCounter;

    public BenchmarkState() {
    }

    @Setup(Level.Trial)
    public final void setup() {
      stampedLock = new StampedLock();
      rwLock = new ReentrantReadWriteLock();
      counter = new AtomicLong();
    }

    @Setup(Level.Iteration)
    public final void iterationSetUp() {
      iterationCounter = new AtomicLong();
    }

    @TearDown(Level.Iteration)
    public final void iterationTearDown(final BenchmarkParams benchmarkParams) {
      final TimeUnit timeUnit = benchmarkParams.getMeasurement().getTime().getTimeUnit();
      final long measurementTime = benchmarkParams.getMeasurement().getTime().convertTo(benchmarkParams.getTimeUnit());
      //      final int measurementIterations = benchmarkParams.getMeasurement().getCount();
      System.out.println(String.format("actualCalculatedThroughput=%s ops/%s", iterationCounter.get() / measurementTime, timeUnit));
    }
  }
}
