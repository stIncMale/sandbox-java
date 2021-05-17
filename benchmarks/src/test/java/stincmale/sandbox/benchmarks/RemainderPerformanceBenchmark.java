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

@TestInstance(Lifecycle.PER_CLASS)
public class RemainderPerformanceBenchmark {
  private static final long DENOMINATOR_POW2 = 1024; // 2^10, must be a power of 2
  private static final long BITWISE_DENOMINATOR_POW2 = DENOMINATOR_POW2 - 1;
  private static final long DENOMINATOR = DENOMINATOR_POW2 - 1;

  public RemainderPerformanceBenchmark() {
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
   * Behaves exactly the same as {@link #ordinaryPow2(ThreadState)} for non-negative numbers,
   * but uses a different approach.
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
