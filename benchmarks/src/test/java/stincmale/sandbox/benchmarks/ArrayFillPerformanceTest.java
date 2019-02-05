package stincmale.sandbox.benchmarks;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import static org.openjdk.jmh.runner.options.TimeValue.milliseconds;
import static stincmale.sandbox.benchmarks.util.JmhOptions.includeBenchmarks;
import static stincmale.sandbox.benchmarks.util.JmhOptions.newOptionsBuilder;

/**
 * Test environment:
 * [single CPU] 3.4 GHz Intel Core i5 (4 cores, 4 hardware threads),
 * [OS] macOS 10.13.6 (17G4015),
 * [JDK] OpenJDK 11.0.1+13 (<a href="https://jdk.java.net/11/">a build from Oracle</a>).
 * <pre>{@code
 * Benchmark                                    (length)   Mode  Cnt       Score       Error   Units
 * ArrayFillPerformanceTest.forLoop0                  10  thrpt   60  153819.217 ±  251.057  ops/ms
 * ArrayFillPerformanceTest.forLoop0                 100  thrpt   60  122815.443 ± 4763.686  ops/ms
 * ArrayFillPerformanceTest.forLoop0                1000  thrpt   60   14017.939 ±   73.214  ops/ms
 * ArrayFillPerformanceTest.forLoop0               10000  thrpt   60     488.964 ±    0.860  ops/ms
 * ArrayFillPerformanceTest.forLoop0              100000  thrpt   60      36.230 ±    0.059  ops/ms
 * ArrayFillPerformanceTest.forLoop1                  10  thrpt   60  184864.108 ± 1095.464  ops/ms
 * ArrayFillPerformanceTest.forLoop1                 100  thrpt   60  126597.924 ± 3812.347  ops/ms
 * ArrayFillPerformanceTest.forLoop1                1000  thrpt   60   14064.293 ±   39.147  ops/ms
 * ArrayFillPerformanceTest.forLoop1               10000  thrpt   60     488.910 ±    0.838  ops/ms
 * ArrayFillPerformanceTest.forLoop1              100000  thrpt   60      36.186 ±    0.068  ops/ms
 * ArrayFillPerformanceTest.forLoopIdx                10  thrpt   60  264339.671 ±  373.718  ops/ms
 * ArrayFillPerformanceTest.forLoopIdx               100  thrpt   60   35202.918 ±  342.154  ops/ms
 * ArrayFillPerformanceTest.forLoopIdx              1000  thrpt   60    3575.148 ±    6.884  ops/ms
 * ArrayFillPerformanceTest.forLoopIdx             10000  thrpt   60     328.179 ±    0.528  ops/ms
 * ArrayFillPerformanceTest.forLoopIdx            100000  thrpt   60      29.442 ±    0.044  ops/ms
 * ArrayFillPerformanceTest.systemArrayCopy0          10  thrpt   60  150318.905 ±  255.848  ops/ms
 * ArrayFillPerformanceTest.systemArrayCopy0         100  thrpt   60   31437.795 ±  258.827  ops/ms
 * ArrayFillPerformanceTest.systemArrayCopy0        1000  thrpt   60    3607.526 ±    5.495  ops/ms
 * ArrayFillPerformanceTest.systemArrayCopy0       10000  thrpt   60     280.327 ±    7.477  ops/ms
 * ArrayFillPerformanceTest.systemArrayCopy0      100000  thrpt   60      21.609 ±    0.185  ops/ms
 * ArrayFillPerformanceTest.systemArrayCopy1          10  thrpt   60  150218.972 ±  344.067  ops/ms
 * ArrayFillPerformanceTest.systemArrayCopy1         100  thrpt   60   31266.816 ±  333.716  ops/ms
 * ArrayFillPerformanceTest.systemArrayCopy1        1000  thrpt   60    3597.043 ±    9.744  ops/ms
 * ArrayFillPerformanceTest.systemArrayCopy1       10000  thrpt   60     287.986 ±    0.826  ops/ms
 * ArrayFillPerformanceTest.systemArrayCopy1      100000  thrpt   60      21.659 ±    0.402  ops/ms
 * ArrayFillPerformanceTest.systemArrayCopyIdx        10  thrpt   60  150267.710 ±  230.910  ops/ms
 * ArrayFillPerformanceTest.systemArrayCopyIdx       100  thrpt   60   31504.801 ±  208.130  ops/ms
 * ArrayFillPerformanceTest.systemArrayCopyIdx      1000  thrpt   60    3605.519 ±    6.613  ops/ms
 * ArrayFillPerformanceTest.systemArrayCopyIdx     10000  thrpt   60     278.295 ±   11.947  ops/ms
 * ArrayFillPerformanceTest.systemArrayCopyIdx    100000  thrpt   60      21.539 ±    0.247  ops/ms
 * }</pre>
 */
@TestInstance(Lifecycle.PER_CLASS)
public class ArrayFillPerformanceTest {
  private static final int ARRAY_SIZE = 100_000;
  private static final long[] arrFilledWithZeros = new long[ARRAY_SIZE];
  private static final long[] arrFilledWithOnes = new long[ARRAY_SIZE];
  private static final long[] arrFilledWithIndices = new long[ARRAY_SIZE];

  static {
    for (int i = 0; i < ARRAY_SIZE; i++) {
      arrFilledWithZeros[i] = 0;//just to handle it the same way we handle other pre-filled arrays
      arrFilledWithOnes[i] = 1;
      arrFilledWithIndices[i] = i;
    }
  }

  public ArrayFillPerformanceTest() {
  }

  @Test
  public void run() throws RunnerException {
    new Runner(newOptionsBuilder(opts -> opts.forks(20)
        .warmupTime(milliseconds(100))
        .warmupIterations(5)
        .measurementTime(milliseconds(100))
        .measurementIterations(3))
        .include(includeBenchmarks(getClass()))
        .shouldDoGC(true)
        .mode(Mode.Throughput)
        .timeUnit(TimeUnit.MILLISECONDS)
        .build())
        .run();
  }

  @Benchmark
  public void forLoop0(final ThreadState state) {
    for (int i = 0; i < state.arr.length; i++) {
      state.arr[i] = 0;
    }
  }

  @Benchmark
  public void systemArrayCopy0(final ThreadState state) {
    System.arraycopy(arrFilledWithZeros, 0, state.arr, 0, state.arr.length);
  }

  @Benchmark
  public void forLoop1(final ThreadState state) {
    for (int i = 0; i < state.arr.length; i++) {
      state.arr[i] = 1;
    }
  }

  @Benchmark
  public void systemArrayCopy1(final ThreadState state) {
    System.arraycopy(arrFilledWithOnes, 0, state.arr, 0, state.arr.length);
  }

  @Benchmark
  public void forLoopIdx(final ThreadState state) {
    for (int i = 0; i < state.arr.length; i++) {
      state.arr[i] = i;
    }
  }

  @Benchmark
  public void systemArrayCopyIdx(final ThreadState state) {
    System.arraycopy(arrFilledWithIndices, 0, state.arr, 0, state.arr.length);
  }

  @State(Scope.Thread)
  public static class ThreadState {
    @Param({"10", "100", "1000", "10000", "100000"})//must not be bigger than ARRAY_SIZE
    private int length;
    private long[] arr;

    @Setup(Level.Iteration)
    public final void setup() {
      arr = new long[length];
    }

    public ThreadState() {
    }
  }
}
