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
import stincmale.sandbox.benchmarks.util.JmhOptions;

/**
 * <pre>{@code
 * Benchmark                         Mode  Cnt    Score   Error   Units
 * PrimitiveDataTypeMathPerformanceTest.divInt    thrpt   80   45.208 ± 0.332  ops/us
 * PrimitiveDataTypeMathPerformanceTest.divLong   thrpt   80   12.736 ± 0.535  ops/us
 * PrimitiveDataTypeMathPerformanceTest.multInt   thrpt   80  194.092 ± 4.246  ops/us
 * PrimitiveDataTypeMathPerformanceTest.multLong  thrpt   80  210.017 ± 3.077  ops/us
 * PrimitiveDataTypeMathPerformanceTest.sumInt    thrpt   80  199.729 ± 3.220  ops/us
 * PrimitiveDataTypeMathPerformanceTest.sumLong   thrpt   80  196.766 ± 3.090  ops/us
 * }</pre>
 */
@TestInstance(Lifecycle.PER_CLASS)
public class PrimitiveDataTypeMathPerformanceTest {
  private static final int[] operandsInt = {-123456789, -1234567, -12345, -123, 123, 12345, 1234567, 123456789};
  private static final long[] operandsLong = {-123456789, -1234567, -12345, -123, 123, 12345, 1234567, 123456789};

  public PrimitiveDataTypeMathPerformanceTest() {
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
  public int sumInt(final ThreadState state) {
    final int l = --state.counterInt;
    int result = 0;
    for (int r : operandsInt) {
      result += l + r;
    }
    return result;
  }

  @Benchmark
  public long sumLong(final ThreadState state) {
    final long l = --state.counterLong;
    long result = 0;
    for (long r : operandsLong) {
      result += l + r;
    }
    return result;
  }

  @Benchmark
  public int multInt(final ThreadState state) {
    final int l = --state.counterInt;
    int result = 0;
    for (int r : operandsInt) {
      result += l * r;
    }
    return result;
  }

  @Benchmark
  public long multLong(final ThreadState state) {
    final long l = --state.counterLong;
    long result = 0;
    for (long r : operandsLong) {
      result += l * r;
    }
    return result;
  }

  @Benchmark
  public int divInt(final ThreadState state) {
    final int l = --state.counterInt;
    int result = 0;
    for (int r : operandsInt) {
      result += l / r;
    }
    return result;
  }

  @Benchmark
  public long divLong(final ThreadState state) {
    final long l = --state.counterLong;
    long result = 0;
    for (long r : operandsLong) {
      result += l / r;
    }
    return result;
  }

  @State(Scope.Thread)
  public static class ThreadState {
    private int counterInt;
    private long counterLong;

    public ThreadState() {
    }

    @Setup(Level.Iteration)
    public final void setup() {
      counterInt = Integer.MAX_VALUE;
      counterLong = Long.MAX_VALUE;
    }
  }
}