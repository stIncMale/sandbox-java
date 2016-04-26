package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class ConcurrentRateSamplerTest {
	public ConcurrentRateSamplerTest() {
	}

	@Test
	public final void getStartNanos() {
		final long startNanos = 123;
		assertEquals(startNanos, new ConcurrentRateSampler(startNanos, Duration.ofSeconds(1)).getStartNanos());
	}

	@Test
	public final void getSampleInterval() {
		final Duration sampleInterval = Duration.ofSeconds(1);
		assertEquals(sampleInterval, new ConcurrentRateSampler(123, sampleInterval).getSampleInterval());
	}

	@Test
	public final void rightSampleWindowBoundary1() {
		final long startNanos = 123;
		assertEquals(startNanos, new ConcurrentRateSampler(startNanos, Duration.ofSeconds(1)).rightSampleWindowBoundary());
	}

	@Test
	public final void rightSampleWindowBoundary2() {
		final ConcurrentRateSampler rs = new ConcurrentRateSampler(0, Duration.ofSeconds(1));
		final long rightmost = 123;
		rs.tick(1, rightmost);
		assertEquals(rightmost, rs.rightSampleWindowBoundary());
	}

	@Test
	public final void numberOfCurrentTicks1() {
		assertEquals(0, new ConcurrentRateSampler(0, Duration.ofSeconds(1)).numberOfCurrentTicks());
	}

	@Test
	public final void numberOfCurrentTicks2() {
		final ConcurrentRateSampler rs = new ConcurrentRateSampler(0, Duration.ofSeconds(5));
		rs.tick(1, TimeUnit.SECONDS.toNanos(1) - 123);
		rs.tick(4, TimeUnit.SECONDS.toNanos(1));
		rs.tick(2, TimeUnit.SECONDS.toNanos(2));
		rs.tick(3, TimeUnit.SECONDS.toNanos(3));
		assertEquals(1 + 4 + 2 + 3, rs.numberOfCurrentTicks());
		rs.tick(0, TimeUnit.SECONDS.toNanos(8));
		assertEquals(1 + 4 + 2 + 3, rs.numberOfCurrentTicks());//right boundary of the sample window must be 3
		rs.tick(-2, TimeUnit.SECONDS.toNanos(5));
		rs.tick(1, TimeUnit.SECONDS.toNanos(3));
		rs.tick(-1, TimeUnit.SECONDS.toNanos(6));
		assertEquals(2 + 3 - 2 + 1 - 1, rs.numberOfCurrentTicks());
	}

	@Test(expected = IllegalArgumentException.class)
	public final void tick1() {
		new ConcurrentRateSampler(-1, Duration.ofSeconds(1))
				.tick(-1, Long.MAX_VALUE);
	}

	@Test(expected = IllegalArgumentException.class)
	public final void tick2() {
		new ConcurrentRateSampler(0, Duration.ofSeconds(1))
				.tick(0, Long.MIN_VALUE);
	}

	@Test(expected = IllegalArgumentException.class)
	public final void tick3() {
		new ConcurrentRateSampler(1, Duration.ofSeconds(1))
				.tick(1, 0);
	}

	@Test
	public final void tick4() {
		new ConcurrentRateSampler(0, Duration.ofSeconds(1))
				.tick(1, 0);
	}

	@Test
	public final void rateAverage() {
		final ConcurrentRateSampler rs = new ConcurrentRateSampler(0, Duration.ofSeconds(5));
		rs.tick(1, TimeUnit.SECONDS.toNanos(1) - 123);
		rs.tick(1, TimeUnit.SECONDS.toNanos(1));
		rs.tick(2, TimeUnit.SECONDS.toNanos(2));
		rs.tick(0, TimeUnit.SECONDS.toNanos(2));
		rs.tick(3, TimeUnit.SECONDS.toNanos(3));
		rs.tick(-2, TimeUnit.SECONDS.toNanos(5));
		rs.tick(1, TimeUnit.SECONDS.toNanos(3));
		rs.tick(-1, TimeUnit.SECONDS.toNanos(6));
		assertDoubleEquals((1d + 1 + 2 + 3 - 2 + 1 - 1) / (6d / 5), rs.rateAverage());
		rs.tick(1000, TimeUnit.SECONDS.toNanos(10));
		assertDoubleEquals((1d + 1 + 2 + 3 - 2 + 1 - 1 + 1000) / (10d / 5), rs.rateAverage());
	}

	@Test
	public final void rateAverage2() {
		final ConcurrentRateSampler rs = new ConcurrentRateSampler(0, Duration.ofSeconds(5));
		rs.tick(1, TimeUnit.SECONDS.toNanos(1) - 123);
		rs.tick(0, TimeUnit.SECONDS.toNanos(1));
		rs.tick(2, TimeUnit.SECONDS.toNanos(2));
		assertDoubleEquals((1d + 2) / (2d / 5), rs.rateAverage(TimeUnit.SECONDS.toNanos(2)));
		rs.tick(3, TimeUnit.SECONDS.toNanos(3));
		rs.tick(-2, TimeUnit.SECONDS.toNanos(5));
		rs.tick(1, TimeUnit.SECONDS.toNanos(3));
		rs.tick(-1, TimeUnit.SECONDS.toNanos(6));
		assertDoubleEquals((1d + 2 + 3 - 2 + 1 - 1) / (10d / 5), rs.rateAverage(TimeUnit.SECONDS.toNanos(10)));
	}

	private static final void assertDoubleEquals(final double extected, final double actual) {
		assertTrue(String.format("Expected %s but was %s", extected, actual),
				Math.abs(extected - actual) <= 0.0000000001);
	}
}