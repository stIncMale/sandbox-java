package stincmale.sandbox.benchmarks;

import java.math.BigInteger;
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
import stincmale.sandbox.benchmarks.util.JmhOptions;

/**
 * <pre>{@code
 * Benchmark                         Mode  Cnt    Score   Error   Units
 * RemainderPerformanceTest.bitwise  thrpt   80  414.174 ± 5.793  ops/us
 * RemainderPerformanceTest.ordinary thrpt   80  290.313 ± 2.108  ops/us
 * RemainderPerformanceTest.pow2     thrpt   80  346.645 ± 4.911  ops/us
 * }</pre>
 */
@TestInstance(Lifecycle.PER_CLASS)
public class RemainderPerformanceTest {
  private static final long denominatorPow2 = BigInteger.valueOf(2).pow(10).longValueExact();
  private static final long bitwiseDenominatorPow2 = denominatorPow2 - 1;
  private static final long denominator = denominatorPow2 - 1;

  public RemainderPerformanceTest() {
  }

  @Test
  public void run() throws RunnerException {
    new Runner(JmhOptions.includingClass(getClass())
        .mode(Mode.Throughput)
        .timeUnit(TimeUnit.MICROSECONDS)
        .build())
        .run();
  }

  @Benchmark
  public long ordinary(final ThreadState state) {
    return (state.counter++) % denominator;
  }

  /**
   * A special case of {@link #ordinary(ThreadState)} when the denominator is a power of 2.
   */
  @Benchmark
  public long pow2(final ThreadState state) {
    return (state.counter++) % denominatorPow2;
  }

  /**
   * Behaves exactly the same as {@link #pow2(ThreadState)} for non-negative numbers, but uses a different approach.
   */
  @Benchmark
  public long bitwise(final ThreadState state) {
    return (state.counter++) & bitwiseDenominatorPow2;
  }

  @State(Scope.Thread)
  public static class ThreadState {
    private long counter;

    public ThreadState() {
    }

    @Setup(Level.Iteration)
    public final void setup() {
      counter = 0;
    }
  }
}
