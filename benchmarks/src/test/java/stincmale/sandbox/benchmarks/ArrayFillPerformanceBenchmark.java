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

@TestInstance(Lifecycle.PER_CLASS)
public class ArrayFillPerformanceBenchmark {
    private static final int ARRAY_SIZE = 100_000;
    private static final long[] ARR_FILLED_WITH_ZEROS;
    private static final long[] ARR_FILLED_WITH_ONES;
    private static final long[] ARR_FILLED_WITH_INDICES;

    static {
        ARR_FILLED_WITH_ZEROS = new long[ARRAY_SIZE];
        ARR_FILLED_WITH_ONES = new long[ARRAY_SIZE];
        ARR_FILLED_WITH_INDICES = new long[ARRAY_SIZE];
        for (int i = 0; i < ARRAY_SIZE; i++) {
            ARR_FILLED_WITH_ONES[i] = 1;
            ARR_FILLED_WITH_INDICES[i] = i;
        }
    }

    public ArrayFillPerformanceBenchmark() {
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
        System.arraycopy(ARR_FILLED_WITH_ZEROS, 0, state.arr, 0, state.arr.length);
    }

    @Benchmark
    public void forLoop1(final ThreadState state) {
        for (int i = 0; i < state.arr.length; i++) {
            state.arr[i] = 1;
        }
    }

    @Benchmark
    public void systemArrayCopy1(final ThreadState state) {
        System.arraycopy(ARR_FILLED_WITH_ONES, 0, state.arr, 0, state.arr.length);
    }

    @Benchmark
    public void forLoopIdx(final ThreadState state) {
        for (int i = 0; i < state.arr.length; i++) {
            state.arr[i] = i;
        }
    }

    @Benchmark
    public void systemArrayCopyIdx(final ThreadState state) {
        System.arraycopy(ARR_FILLED_WITH_INDICES, 0, state.arr, 0, state.arr.length);
    }

    @State(Scope.Thread)
    public static class ThreadState {
        // must not be bigger than ARRAY_SIZE
        @Param({"10", "100", "1000", "10000", "100000"})
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
