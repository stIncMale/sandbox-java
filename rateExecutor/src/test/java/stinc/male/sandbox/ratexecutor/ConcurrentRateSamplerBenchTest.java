package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

public class ConcurrentRateSamplerBenchTest {
	public ConcurrentRateSamplerBenchTest() {
	}

	@Test
	public void launchBenchmark() throws Exception {
		Options opt = new OptionsBuilder()
				.include(getClass().getName())
				.mode (Mode.Throughput)
				.timeUnit(TimeUnit.MILLISECONDS)
				.warmupTime(TimeValue.seconds(2))
				.warmupIterations(3)
				.measurementTime(TimeValue.seconds(2))
				.measurementIterations(3)
				.threads(4)
				.forks(2)
				.shouldFailOnError(true)
				.shouldDoGC(true)
				.build();
		new Runner(opt).run();
	}

	@Benchmark
	public void tick(final RateSamplerContainer rateSamplerContainer) throws Exception {
		final long tNanos = TimeUnit.MILLISECONDS.toNanos(System.currentTimeMillis());
		rateSamplerContainer.value.tick(1, tNanos);
	}

	@State(Scope.Benchmark)
	public static class RateSamplerContainer {
		volatile RateSampler value;

		public RateSamplerContainer() {
		}

		@Setup
		public final void setup() {
			value = new ConcurrentRateSampler(System.nanoTime(), Duration.ofSeconds(1));
		}
	}
}