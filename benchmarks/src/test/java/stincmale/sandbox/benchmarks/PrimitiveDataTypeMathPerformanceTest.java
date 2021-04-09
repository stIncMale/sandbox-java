package stincmale.sandbox.benchmarks;

import java.util.Random;
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
public class PrimitiveDataTypeMathPerformanceTest {
  private static final int NUMBER_OF_VALUES = 64;//2^6, must be a power of 2
  private static final int[] operandsInt;
  private static final float[] operandsFloat;
  private static final long[] operandsLong;
  private static final double[] operandsDouble;

  static {
    final Random rnd = new Random(1L);
    operandsInt = new int[NUMBER_OF_VALUES * 2];
    operandsFloat = new float[NUMBER_OF_VALUES * 2];
    operandsLong = new long[NUMBER_OF_VALUES];
    operandsDouble = new double[NUMBER_OF_VALUES];
    for (int i = 0; i < NUMBER_OF_VALUES; i++) {
      {//fill operandsInt
        int rndInt = rnd.nextInt();
        if (rndInt > Integer.MIN_VALUE / 2 && rndInt < Integer.MAX_VALUE / 2) {
          rndInt *= 2;
        }
        /*
         * The index for operandsInt/operandsFloat is i * 2
         * to make sure that the number of both int/float and long/double array elements is about the same for each cache line.
         * This of course only works if the underlying software and hardware actually uses two times as much memory
         * for long/double comparing to int/float and allocates all array elements contiguously in memory.
         */
        operandsInt[i * 2] = rndInt;
      }
      {//fill operandsFloat
        float rndFloat = rnd.nextFloat();
        if (rndFloat > Float.MIN_VALUE / 2 && rndFloat < Float.MAX_VALUE / 2) {
          rndFloat *= 2;
        }
        operandsFloat[i * 2] = rndFloat;
      }
      {//fill operandsLong
        long rndLong = rnd.nextLong();
        if (rndLong > Long.MIN_VALUE / 2 && rndLong < Long.MAX_VALUE / 2) {
          rndLong *= 2;
        }
        operandsLong[i] = rndLong;
      }
      {//fill operandsDouble
        double rndDouble = rnd.nextDouble();
        if (rndDouble > Double.MIN_VALUE / 2 && rndDouble < Double.MAX_VALUE / 2) {
          rndDouble *= 2;
        }
        operandsDouble[i] = rndDouble;
      }
    }
  }

  public PrimitiveDataTypeMathPerformanceTest() {
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
  public int baselineInt(final ThreadState state) {
    final int idx = state.idx;
    final int nextIdx = nextIdxIntFloat(idx);
    state.idx = nextIdx;
    return operandsInt[nextIdx];
  }

  @Benchmark
  public float baselineFloat(final ThreadState state) {
    final int idx = state.idx;
    final int nextIdx = nextIdxIntFloat(idx);
    state.idx = nextIdx;
    return operandsFloat[nextIdx];
  }

  @Benchmark
  public long baselineLong(final ThreadState state) {
    final int idx = state.idx;
    final int nextIdx = nextIdxLongDouble(idx);
    state.idx = nextIdx;
    return operandsLong[nextIdx];
  }

  @Benchmark
  public double baselineDouble(final ThreadState state) {
    final int idx = state.idx;
    final int nextIdx = nextIdxLongDouble(idx);
    state.idx = nextIdx;
    return operandsDouble[nextIdx];
  }

  @Benchmark
  public int addInt(final ThreadState state) {
    final int idx = state.idx;
    final int nextIdx = nextIdxIntFloat(idx);
    state.idx = nextIdx;
    return operandsInt[idx] + operandsInt[nextIdx];
  }

  @Benchmark
  public int multInt(final ThreadState state) {
    final int idx = state.idx;
    final int nextIdx = nextIdxIntFloat(idx);
    state.idx = nextIdx;
    return operandsInt[idx] * operandsInt[nextIdx];
  }

  @Benchmark
  public int divInt(final ThreadState state) {
    final int idx = state.idx;
    final int nextIdx = nextIdxIntFloat(idx);
    state.idx = nextIdx;
    return operandsInt[idx] / operandsInt[nextIdx];
  }

  @Benchmark
  public int remInt(final ThreadState state) {
    final int idx = state.idx;
    final int nextIdx = nextIdxIntFloat(idx);
    state.idx = nextIdx;
    return operandsInt[idx] % operandsInt[nextIdx];
  }

  @Benchmark
  public boolean eqInt(final ThreadState state) {
    final int idx = state.idx;
    final int nextIdx = nextIdxIntFloat(idx);
    state.idx = nextIdx;
    return operandsInt[idx] == operandsInt[nextIdx];
  }

  @Benchmark
  public boolean ltInt(final ThreadState state) {
    final int idx = state.idx;
    final int nextIdx = nextIdxIntFloat(idx);
    state.idx = nextIdx;
    return operandsInt[idx] < operandsInt[nextIdx];
  }

  @Benchmark
  public float addFloat(final ThreadState state) {
    final int idx = state.idx;
    final int nextIdx = nextIdxIntFloat(idx);
    state.idx = nextIdx;
    return operandsFloat[idx] + operandsFloat[nextIdx];
  }

  @Benchmark
  public float multFloat(final ThreadState state) {
    final int idx = state.idx;
    final int nextIdx = nextIdxIntFloat(idx);
    state.idx = nextIdx;
    return operandsFloat[idx] * operandsFloat[nextIdx];
  }

  @Benchmark
  public float divFloat(final ThreadState state) {
    final int idx = state.idx;
    final int nextIdx = nextIdxIntFloat(idx);
    state.idx = nextIdx;
    return operandsFloat[idx] / operandsFloat[nextIdx];
  }

  @Benchmark
  public float remFloat(final ThreadState state) {
    final int idx = state.idx;
    final int nextIdx = nextIdxIntFloat(idx);
    state.idx = nextIdx;
    return operandsFloat[idx] % operandsFloat[nextIdx];
  }

  @Benchmark
  public boolean eqFloat(final ThreadState state) {
    final int idx = state.idx;
    final int nextIdx = nextIdxIntFloat(idx);
    state.idx = nextIdx;
    return operandsFloat[idx] == operandsFloat[nextIdx];
  }

  @Benchmark
  public boolean ltFloat(final ThreadState state) {
    final int idx = state.idx;
    final int nextIdx = nextIdxIntFloat(idx);
    state.idx = nextIdx;
    return operandsFloat[idx] < operandsFloat[nextIdx];
  }

  @Benchmark
  public long addLong(final ThreadState state) {
    final int idx = state.idx;
    final int nextIdx = nextIdxLongDouble(idx);
    state.idx = nextIdx;
    return operandsLong[idx] + operandsLong[nextIdx];
  }

  @Benchmark
  public long multLong(final ThreadState state) {
    final int idx = state.idx;
    final int nextIdx = nextIdxLongDouble(idx);
    state.idx = nextIdx;
    return operandsLong[idx] * operandsLong[nextIdx];
  }

  @Benchmark
  public long divLong(final ThreadState state) {
    final int idx = state.idx;
    final int nextIdx = nextIdxLongDouble(idx);
    state.idx = nextIdx;
    return operandsLong[idx] / operandsLong[nextIdx];
  }

  @Benchmark
  public long remLong(final ThreadState state) {
    final int idx = state.idx;
    final int nextIdx = nextIdxLongDouble(idx);
    state.idx = nextIdx;
    return operandsLong[idx] % operandsLong[nextIdx];
  }

  @Benchmark
  public boolean eqLong(final ThreadState state) {
    final int idx = state.idx;
    final int nextIdx = nextIdxLongDouble(idx);
    state.idx = nextIdx;
    return operandsLong[idx] == operandsLong[nextIdx];
  }

  @Benchmark
  public boolean ltLong(final ThreadState state) {
    final int idx = state.idx;
    final int nextIdx = nextIdxLongDouble(idx);
    state.idx = nextIdx;
    return operandsLong[idx] < operandsLong[nextIdx];
  }

  @Benchmark
  public double addDouble(final ThreadState state) {
    final int idx = state.idx;
    final int nextIdx = nextIdxLongDouble(idx);
    state.idx = nextIdx;
    return operandsDouble[idx] + operandsDouble[nextIdx];
  }

  @Benchmark
  public double multDouble(final ThreadState state) {
    final int idx = state.idx;
    final int nextIdx = nextIdxLongDouble(idx);
    state.idx = nextIdx;
    return operandsDouble[idx] * operandsDouble[nextIdx];
  }

  @Benchmark
  public double divDouble(final ThreadState state) {
    final int idx = state.idx;
    final int nextIdx = nextIdxLongDouble(idx);
    state.idx = nextIdx;
    return operandsDouble[idx] / operandsDouble[nextIdx];
  }

  @Benchmark
  public double remDouble(final ThreadState state) {
    final int idx = state.idx;
    final int nextIdx = nextIdxLongDouble(idx);
    state.idx = nextIdx;
    return operandsDouble[idx] % operandsDouble[nextIdx];
  }

  @Benchmark
  public boolean eqDouble(final ThreadState state) {
    final int idx = state.idx;
    final int nextIdx = nextIdxLongDouble(idx);
    state.idx = nextIdx;
    return operandsDouble[idx] == operandsDouble[nextIdx];
  }

  @Benchmark
  public boolean ltDouble(final ThreadState state) {
    final int idx = state.idx;
    final int nextIdx = nextIdxLongDouble(idx);
    state.idx = nextIdx;
    return operandsDouble[idx] < operandsDouble[nextIdx];
  }

  @State(Scope.Thread)
  public static class ThreadState {
    private int idx;

    public ThreadState() {
    }

    @Setup(Level.Iteration)
    public final void setup() {
      idx = 0;
    }
  }

  private static final int nextIdx(final int idx, final int step) {
    return (idx + step) & (NUMBER_OF_VALUES - 1);//(idx + step) % NUMBER_OF_VALUES
  }

  private static final int nextIdxIntFloat(final int idx) {
    return nextIdx(idx, 2);
  }

  private static final int nextIdxLongDouble(final int idx) {
    return nextIdx(idx, 1);
  }
}
