package stincmale.sandbox.benchmarks;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import static org.openjdk.jmh.runner.options.TimeValue.milliseconds;

/**
 * # JMH version: 1.21
 * # VM version: JDK 12, OpenJDK 64-Bit Server VM, 12+33
 * # VM invoker: /Library/Java/JavaVirtualMachines/jdk-12.jdk/Contents/Home/bin/java
 * # VM options: -Xfuture --illegal-access=deny -Xms1024m -Xmx1024m
 * # Warmup: <none>
 * # Measurement: 2 iterations, 1000 ms each
 * # Timeout: 1000000000 ms per iteration
 * # Threads: 2 threads, will synchronize iterations
 * # Benchmark mode: Throughput, ops/time
 * # Benchmark: stincmale.sandbox.benchmarks.TmpTest.monitorSleep
 *
 * # Run progress: 0.00% complete, ETA 00:00:06
 * # Fork: 1 of 1
 * Iteration   1:
 * # non-JMH: throughput 0.997 ops/s (4 ops / 4.013 s)
 * 1.330 ops/s
 * Iteration   2:
 * # non-JMH: throughput 0.999 ops/s (4 ops / 4.005 s)
 * 0.999 ops/s
 *
 *
 * Result "stincmale.sandbox.benchmarks.TmpTest.monitorSleep":
 *   1.164 ops/s
 *
 *
 * # JMH version: 1.21
 * # VM version: JDK 12, OpenJDK 64-Bit Server VM, 12+33
 * # VM invoker: /Library/Java/JavaVirtualMachines/jdk-12.jdk/Contents/Home/bin/java
 * # VM options: -Xfuture --illegal-access=deny -Xms1024m -Xmx1024m
 * # Warmup: <none>
 * # Measurement: 2 iterations, 1000 ms each
 * # Timeout: 1000000000 ms per iteration
 * # Threads: 2 threads, will synchronize iterations
 * # Benchmark mode: Throughput, ops/time
 * # Benchmark: stincmale.sandbox.benchmarks.TmpTest.sleep
 *
 * # Run progress: 33.33% complete, ETA 00:00:17
 * # Fork: 1 of 1
 * Iteration   1:
 * # non-JMH: throughput 1.994 ops/s (4 ops / 2.006 s)
 * 1.994 ops/s
 * Iteration   2:
 * # non-JMH: throughput 1.660 ops/s (5 ops / 3.012 s)
 * 1.991 ops/s
 *
 *
 * Result "stincmale.sandbox.benchmarks.TmpTest.sleep":
 *   1.992 ops/s
 *
 *
 * # JMH version: 1.21
 * # VM version: JDK 12, OpenJDK 64-Bit Server VM, 12+33
 * # VM invoker: /Library/Java/JavaVirtualMachines/jdk-12.jdk/Contents/Home/bin/java
 * # VM options: -Xfuture --illegal-access=deny -Xms1024m -Xmx1024m
 * # Warmup: <none>
 * # Measurement: 2 iterations, 1000 ms each
 * # Timeout: 1000000000 ms per iteration
 * # Threads: 2 threads, will synchronize iterations
 * # Benchmark mode: Throughput, ops/time
 * # Benchmark: stincmale.sandbox.benchmarks.TmpTest.writeLockSleep
 *
 * # Run progress: 66.67% complete, ETA 00:00:06
 * # Fork: 1 of 1
 * Iteration   1:
 * # non-JMH: throughput 0.995 ops/s (4 ops / 4.020 s)
 * 1.326 ops/s
 * Iteration   2:
 * # non-JMH: throughput 0.998 ops/s (13 ops / 13.031 s)
 * 1.081 ops/s
 *
 *
 * Result "stincmale.sandbox.benchmarks.TmpTest.writeLockSleep":
 *   1.204 ops/s
 *
 *
 * # Run complete. Total time: 00:00:31
 *
 * REMEMBER: The numbers below are just data. To gain reusable insights, you need to follow up on
 * why the numbers are the way they are. Use profilers (see -prof, -lprof), design factorial
 * experiments, perform baseline and negative tests that provide experimental control, make sure
 * the benchmarking environment is safe on JVM/OS/HW level, ask for reviews from the domain experts.
 * Do not assume the numbers tell you what you want them to tell.
 *
 * Benchmark                Mode  Cnt  Score   Error  Units
 * TmpTest.monitorSleep    thrpt    2  1.164          ops/s
 * TmpTest.sleep           thrpt    2  1.992          ops/s
 * TmpTest.writeLockSleep  thrpt    2  1.204          ops/s
 *
 * Process finished with exit code 0
 */
public class TmpTest {
  public TmpTest() {
  }

  public final void runThroughputBenchmarks(final int numberOfThreads) throws RunnerException {
    final OptionsBuilder opts = new OptionsBuilder();
    opts.jvmArgs("-Xverify:all", "--illegal-access=deny", "-Xms1024m", "-Xmx1024m")
        .shouldDoGC(false)
        .syncIterations(true)
        .shouldFailOnError(true)
        .timeout(milliseconds(1000_000_000))
        .forks(1)
        .warmupTime(milliseconds(1000))
        .warmupIterations(0)
        .measurementTime(milliseconds(1000))
        .measurementIterations(2)
        .include(getClass().getName() + ".*")
        .mode(Mode.Throughput)
        .timeUnit(TimeUnit.SECONDS)
        .threads(numberOfThreads);
    new Runner(opts.build()).run();
  }

  @Benchmark
  public final void sleep(final BenchmarkState s) throws InterruptedException {
    s.opsBeginCounter.increment();
    {
      Thread.sleep(1000);
    }
    s.opsEndCounter.increment();
  }

  @Benchmark
  public final void monitorSleep(final BenchmarkState s) throws InterruptedException {
    s.opsBeginCounter.increment();
    {
      synchronized (s.monitor) {
        Thread.sleep(1000);
      }
    }
    s.opsEndCounter.increment();
  }

  @Benchmark
  public final void writeLockSleep(final BenchmarkState s) throws InterruptedException {
    s.opsBeginCounter.increment();
    {
      final ReentrantReadWriteLock.WriteLock wLock = s.rwLock.writeLock();
      wLock.lock();
      try {
        Thread.sleep(1000);
      } finally {
        wLock.unlock();
      }
    }
    s.opsEndCounter.increment();
  }

  @State(Scope.Benchmark)
  public static class BenchmarkState {
    private Object monitor;
    private ReentrantReadWriteLock rwLock;
    private LongAdder opsBeginCounter;
    private LongAdder opsEndCounter;
    private long startNanos;//JMH probably registers a later instant as the start time

    public BenchmarkState() {
    }

    @Setup(Level.Iteration)
    public final void setup() {
      monitor = new Object();
      rwLock = new ReentrantReadWriteLock();
      opsBeginCounter = new LongAdder();
      opsEndCounter = new LongAdder();
      startNanos = System.nanoTime();
    }

    @TearDown(Level.Iteration)
    public final void tearDown() {
      final long stopNanos = System.nanoTime();//JMH probably registers an earlier instant as the start time
      final double durationSeconds = ((double)(stopNanos - startNanos)) / TimeUnit.SECONDS.toNanos(1);//JMH probably registers a smaller duration
      final long opsBeginCount = opsBeginCounter.sum();
      final double throughputBegin = opsBeginCount / durationSeconds;
      final long opsEndCount = opsEndCounter.sum();
      if (opsEndCount != opsBeginCount) {//should never happen, but I added just in case since the measurements are incorrect
        throw new AssertionError(String.format("A benchmark method threw an exception that was swallowed by JMH %d times",
            opsBeginCount - opsEndCount));
      }
      System.out.printf("%n# non-JMH: throughput %.3f ops/s (%d ops / %.3f s)%n", throughputBegin, opsBeginCount, durationSeconds);
    }
  }

  public static final void main(final String... args) throws RunnerException {
    final int numberOfThreads = 2;
    final TmpTest test = new TmpTest();
    test.runThroughputBenchmarks(numberOfThreads);
  }
}
