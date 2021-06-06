package stincmale.sandbox.benchmarks;

import java.util.Locale;
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

public class TmpBenchmark {
    public TmpBenchmark() {
    }

    public final void runThroughputBenchmarks(final int numberOfThreads) throws RunnerException {
        final OptionsBuilder opts = new OptionsBuilder();
        opts.jvmArgs("-Xverify:all", "-Xms1024m", "-Xmx1024m")
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

    public static final void main(final String... args) throws RunnerException {
        final int numberOfThreads = 2;
        final TmpBenchmark test = new TmpBenchmark();
        test.runThroughputBenchmarks(numberOfThreads);
    }

    @State(Scope.Benchmark)
    public static class BenchmarkState {
        private Object monitor;
        private ReentrantReadWriteLock rwLock;
        private LongAdder opsBeginCounter;
        private LongAdder opsEndCounter;
        private long startNanos;// JMH probably registers a later instant as the start time

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
            // JMH probably registers an earlier instant as the start time
            final long stopNanos = System.nanoTime();
            // JMH probably registers a smaller duration
            final double durationSeconds =
                    ((double)(stopNanos - startNanos)) / TimeUnit.SECONDS.toNanos(1);
            final long opsBeginCount = opsBeginCounter.sum();
            final double throughputBegin = opsBeginCount / durationSeconds;
            final long opsEndCount = opsEndCounter.sum();
            // should never happen, but I added just in case since the measurements are incorrect
            if (opsEndCount != opsBeginCount) {
                throw new AssertionError(String.format(Locale.ROOT,
                        "A benchmark method threw an exception that was swallowed by JMH %d times",
                        opsBeginCount - opsEndCount));
            }
            System.out.printf(Locale.ROOT, "%n# non-JMH: throughput %.3f ops/s (%d ops / %.3f s)%n",
                    throughputBegin, opsBeginCount, durationSeconds);
        }
    }
}
