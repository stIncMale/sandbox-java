package stincmale.sandbox.benchmarks.tmp;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import static org.openjdk.jmh.runner.options.TimeValue.milliseconds;
import static stincmale.sandbox.benchmarks.util.JmhOptions.includeClass;
import static stincmale.sandbox.benchmarks.util.JmhOptions.jvmArgsDisableGc;
import static stincmale.sandbox.benchmarks.util.JmhOptions.newOptionsBuilder;

@TestInstance(Lifecycle.PER_CLASS)
public class LambdaPerformanceTest {
    public LambdaPerformanceTest() {
    }

    @Test
    public void run() throws RunnerException {
        new Runner(newOptionsBuilder(opts -> opts.forks(20)
                .warmupTime(milliseconds(100))
                .warmupIterations(10)
                .measurementTime(milliseconds(100))
                .measurementIterations(3))
                .include(includeClass(getClass()))
                .jvmArgsAppend(jvmArgsDisableGc())
                .mode(Mode.Throughput)
                .timeUnit(TimeUnit.MILLISECONDS)
                .build())
                .run();
    }

    @Benchmark
    public Integer rnd() {
        return ThreadLocalRandom.current()
                .nextInt();
    }

//    @Benchmark
//    public Integer rndWithLambdaExpression() {
//        return get(() -> ThreadLocalRandom.current()
//                .nextInt());
//    }
//
//    @Benchmark
//    public Integer rndWithAnonymousClass() {
//        return get(new Supplier<Integer>() {
//            @Override
//            public final Integer get() {
//                return ThreadLocalRandom.current()
//                        .nextInt();
//            }
//        });
//    }
//
//    private static final Integer get(final Supplier<Integer> s) {
//        return s.get();
//    }
}
