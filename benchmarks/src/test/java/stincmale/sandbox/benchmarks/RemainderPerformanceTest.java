package stincmale.sandbox.benchmarks;

import java.util.concurrent.TimeUnit;
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
import static stincmale.sandbox.benchmarks.util.JmhOptions.jvmArgsDisableGc;
import static stincmale.sandbox.benchmarks.util.JmhOptions.newOptionsBuilder;

/**
 * Test environment:
 * [single CPU] 3.4 GHz Intel Core i5 (4 cores, 4 hardware threads),
 * [OS] macOS 10.13.6 (17G4015),
 * [JDK] OpenJDK 11.0.1+13 (<a href="https://jdk.java.net/11/">a build from Oracle</a>).
 * <pre>{@code
 * Benchmark                              Mode  Cnt  Score   Error  Units
 * RemainderPerformanceTest.baseline      avgt   30  2.447 ± 0.012  ns/op
 * RemainderPerformanceTest.bitwise       avgt   30  2.457 ± 0.007  ns/op
 * RemainderPerformanceTest.ordinaryPow2  avgt   30  2.901 ± 0.008  ns/op
 * RemainderPerformanceTest.ordinary      avgt   30  3.482 ± 0.010  ns/op
 * }</pre>
 */
@TestInstance(Lifecycle.PER_CLASS)
public class RemainderPerformanceTest {
  private static final long DENOMINATOR_POW2 = 1024;//2^10, must be a power of 2
  private static final long BITWISE_DENOMINATOR_POW2 = DENOMINATOR_POW2 - 1;
  private static final long DENOMINATOR = DENOMINATOR_POW2 - 1;

  public RemainderPerformanceTest() {
  }

  @Test
  public void run() throws RunnerException {
    new Runner(newOptionsBuilder(opts -> opts.forks(10)
        .warmupTime(milliseconds(100))
        .warmupIterations(10)
        .measurementTime(milliseconds(100))
        .measurementIterations(3))
        .include(includeBenchmarks(getClass()))
        .jvmArgsAppend(jvmArgsDisableGc())
        .mode(Mode.AverageTime)
        .timeUnit(TimeUnit.NANOSECONDS)
        .build())
        .run();
  }

  @Benchmark
  public long baseline(final ThreadState state) {
    return state.counter++;
  }

  @Benchmark
  public long ordinary(final ThreadState state) {
    return (state.counter++) % DENOMINATOR;
  }

  /**
   * A special case of {@link #ordinary(ThreadState)} when the denominator is a power of 2.
   */
  @Benchmark
  public long ordinaryPow2(final ThreadState state) {
    return (state.counter++) % DENOMINATOR_POW2;
  }

  /**
   * Behaves exactly the same as {@link #ordinaryPow2(ThreadState)} for non-negative numbers, but uses a different approach.
   */
  @Benchmark
  public long bitwise(final ThreadState state) {
    return (state.counter++) & BITWISE_DENOMINATOR_POW2;
  }

  @State(Scope.Thread)
  public static class ThreadState {
    private long counter;

    public ThreadState() {
    }

    @Setup(Level.Iteration)
    public final void setup() {
      counter = Long.MAX_VALUE / 2;
    }
  }
}
