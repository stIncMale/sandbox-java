package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class SimpleRateSamplerTest {
	public SimpleRateSamplerTest() {
	}

	@Test
	public final void getStartNanos() {
		final long startNanos = 123;
		assertEquals(startNanos, new SimpleRateSampler(startNanos, Duration.ofSeconds(1)).getStartNanos());
	}

	@Test
	public final void getSampleInterval() {
		final Duration sampleInterval = Duration.ofSeconds(1);
		assertEquals(sampleInterval, new SimpleRateSampler(123, sampleInterval).getSampleInterval());
	}

	@Test
	public final void rightSampleWindowBoundary1() {
		final long startNanos = 123;
		assertEquals(startNanos, new SimpleRateSampler(startNanos, Duration.ofSeconds(1)).rightSampleWindowBoundary());
	}

	@Test
	public final void rightSampleWindowBoundary2() {
		final SimpleRateSampler rs = new SimpleRateSampler(0, Duration.ofSeconds(1));
		final long rightmost = 123;
		rs.tick(1, rightmost);
		assertEquals(rightmost, rs.rightSampleWindowBoundary());
	}

	@Test
	public final void ticksCount1() {
		assertEquals(0, new SimpleRateSampler(0, Duration.ofSeconds(1)).ticksCount());
	}

	@Test
	public final void ticksCount2() {
		final SimpleRateSampler rs = new SimpleRateSampler(-5, Duration.ofSeconds(5));
		rs.tick(1, TimeUnit.SECONDS.toNanos(1) - 123);
		rs.tick(4, TimeUnit.SECONDS.toNanos(1));
		rs.tick(2, TimeUnit.SECONDS.toNanos(2));
		rs.tick(3, TimeUnit.SECONDS.toNanos(3));
		assertEquals(1 + 4 + 2 + 3, rs.ticksCount());
		rs.tick(0, TimeUnit.SECONDS.toNanos(8));
		assertEquals(1 + 4 + 2 + 3, rs.ticksCount());
		rs.tick(-2, TimeUnit.SECONDS.toNanos(5));
		rs.tick(1, TimeUnit.SECONDS.toNanos(3));
		rs.tick(-1, TimeUnit.SECONDS.toNanos(6));
		assertEquals(2 + 3 - 2 + 1 - 1, rs.ticksCount());
	}

	@Test
	public final void ticksTotalCount1() {
		assertEquals(0, new SimpleRateSampler(0, Duration.ofSeconds(1)).ticksTotalCount());
	}

	@Test
	public final void ticksTotalCount2() {
		final SimpleRateSampler rs = new SimpleRateSampler(Long.MAX_VALUE, Duration.ofSeconds(5));
		rs.tick(1, Long.MIN_VALUE);
		assertEquals(1, rs.ticksTotalCount());
		assertEquals(rs.ticksCount(), rs.ticksTotalCount());
		rs.tick(4, -6_000_000_000L);
		rs.tick(2, -5_500_000_000L);
		assertEquals(1 + 4 + 2, rs.ticksTotalCount());
	}

	@Test(expected = IllegalArgumentException.class)
	public final void tick1() {
		new SimpleRateSampler(-1, Duration.ofSeconds(1))
				.tick(-1, Long.MAX_VALUE);
	}

	@Test(expected = IllegalArgumentException.class)
	public final void tick2() {
		new SimpleRateSampler(0, Duration.ofSeconds(1))
				.tick(0, Long.MIN_VALUE);
	}

	@Test(expected = IllegalArgumentException.class)
	public final void tick3() {
		new SimpleRateSampler(1, Duration.ofSeconds(1))
				.tick(1, 0);
	}

	@Test
	public final void tick4() {
		new SimpleRateSampler(0, Duration.ofSeconds(1))
				.tick(1, 1);
	}

	@Test
	public final void rateAverage1() {
		assertDoubleEquals(0, new SimpleRateSampler(0, Duration.ofSeconds(1)).rateAverage());
	}

	@Test
	public final void rateAverage2() {
		assertDoubleEquals(0, new SimpleRateSampler(0, Duration.ofSeconds(1)).rateAverage(0));
	}

	@Test
	public final void rateAverage3() {
		final SimpleRateSampler rs = new SimpleRateSampler(0, Duration.ofSeconds(5));
		rs.tick(1, TimeUnit.SECONDS.toNanos(1) - 123);
		rs.tick(1, TimeUnit.SECONDS.toNanos(1));
		rs.tick(2, TimeUnit.SECONDS.toNanos(2));
		rs.tick(0, TimeUnit.SECONDS.toNanos(2));
		rs.tick(3, TimeUnit.SECONDS.toNanos(3));
		rs.tick(-2, TimeUnit.SECONDS.toNanos(5));
		rs.tick(1, TimeUnit.SECONDS.toNanos(3));
		rs.tick(-1, TimeUnit.SECONDS.toNanos(6));
		rs.tick(4, 1);
		assertDoubleEquals((1d + 1 + 2 + 3 - 2 + 1 - 1 + 4) / (6d / 5), rs.rateAverage());
		assertDoubleEquals((1d + 1 + 2 + 3 - 2 + 1 - 1 + 4) / (6d / 2), rs.rateAverage(Duration.ofSeconds(2)));
		rs.tick(1000, TimeUnit.SECONDS.toNanos(10));
		assertDoubleEquals((1d + 1 + 2 + 3 - 2 + 1 - 1 + 4 + 1000) / (10d / 5), rs.rateAverage());
		assertDoubleEquals((1d + 1 + 2 + 3 - 2 + 1 - 1 + 4 + 1000) / (10d / 2), rs.rateAverage(Duration.ofSeconds(2)));
		assertDoubleEquals(rs.rateAverage(rs.rightSampleWindowBoundary()), rs.rateAverage());
		assertDoubleEquals(rs.rateAverage(rs.rightSampleWindowBoundary(), Duration.ofSeconds(2)), rs.rateAverage(Duration.ofSeconds(2)));
	}

	@Test
	public final void rateAverage4() {
		final SimpleRateSampler rs = new SimpleRateSampler(0, Duration.ofSeconds(5));
		rs.tick(4, 0);
		rs.tick(1, TimeUnit.SECONDS.toNanos(1) - 123);
		rs.tick(0, TimeUnit.SECONDS.toNanos(1));
		rs.tick(2, TimeUnit.SECONDS.toNanos(2));
		assertDoubleEquals((4d + 1 + 2) / (2d / 5), rs.rateAverage(TimeUnit.SECONDS.toNanos(2)));
		assertDoubleEquals((4d + 1 + 2) / (2d / 3), rs.rateAverage(TimeUnit.SECONDS.toNanos(2), Duration.ofSeconds(3)));
		rs.tick(3, TimeUnit.SECONDS.toNanos(3));
		rs.tick(-2, TimeUnit.SECONDS.toNanos(5));
		rs.tick(1, TimeUnit.SECONDS.toNanos(3));
		rs.tick(-1, TimeUnit.SECONDS.toNanos(6));
		assertDoubleEquals((4d + 1 + 2 + 3 - 2 + 1 - 1) / (10d / 5), rs.rateAverage(TimeUnit.SECONDS.toNanos(10)));
		assertDoubleEquals((4d + 1 + 2 + 3 - 2 + 1 - 1) / (10d / 3), rs.rateAverage(TimeUnit.SECONDS.toNanos(10), Duration.ofSeconds(3)));
	}

	@Test
	public final void rate1() {
		assertDoubleEquals(0, new SimpleRateSampler(0, Duration.ofSeconds(1)).rate());
	}

	@Test
	public final void rate2() {
		assertDoubleEquals(0, new SimpleRateSampler(0, Duration.ofSeconds(1)).rate(0));
	}

	@Test
	public final void rate3() {
		final SimpleRateSampler rs = new SimpleRateSampler(TimeUnit.SECONDS.toNanos(-1), Duration.ofSeconds(3));
		rs.tick(1, 0);
		rs.tick(1, TimeUnit.SECONDS.toNanos(1) - 123);
		assertDoubleEquals(1 + 1, rs.rate());
		//noinspection PointlessArithmeticExpression
		assertDoubleEquals((1d + 1) / (3 / 1), rs.rate(Duration.ofSeconds(1)));
		rs.tick(0, TimeUnit.SECONDS.toNanos(1));
		rs.tick(2, TimeUnit.SECONDS.toNanos(2));
		assertDoubleEquals(1 + 1 + 2, rs.rate());
		//noinspection PointlessArithmeticExpression
		assertDoubleEquals((1d + 1 + 2) / (3 / 1), rs.rate(Duration.ofSeconds(1)));
		rs.tick(3, TimeUnit.SECONDS.toNanos(3));
		assertDoubleEquals(1 + 2 + 3, rs.rate());
		rs.tick(-2, TimeUnit.SECONDS.toNanos(5));
		rs.tick(1, TimeUnit.SECONDS.toNanos(3));
		rs.tick(-1, TimeUnit.SECONDS.toNanos(6));
		assertDoubleEquals(-2 - 1, rs.rate());
		//noinspection PointlessArithmeticExpression
		assertDoubleEquals((-2d - 1) / (3 / 1), rs.rate(Duration.ofSeconds(1)));
		assertDoubleEquals(rs.ticksCount(), rs.rate());
		assertDoubleEquals(rs.rate(rs.rightSampleWindowBoundary()), rs.rate());
		assertDoubleEquals(rs.rate(rs.rightSampleWindowBoundary(), Duration.ofSeconds(1)), rs.rate(Duration.ofSeconds(1)));
	}

	@Test
	public final void rate4() {
		final SimpleRateSampler rs = new SimpleRateSampler(-2, Duration.ofSeconds(3));
		rs.tick(1, -1);
		rs.tick(3, 0);
		rs.tick(1, TimeUnit.SECONDS.toNanos(1));
		rs.tick(0, TimeUnit.SECONDS.toNanos(1));
		rs.tick(2, TimeUnit.SECONDS.toNanos(2));
		assertDoubleEquals((1d + 3 + 1 + 2), rs.rate(TimeUnit.SECONDS.toNanos(2)));
		assertDoubleEquals((1d + 3 + 1 + 2) / (3 / 1.5), rs.rate(TimeUnit.SECONDS.toNanos(2), Duration.ofMillis(1500)));
		rs.tick(3, TimeUnit.SECONDS.toNanos(3));
		assertDoubleEquals((1 + 2 + 3), rs.rate(TimeUnit.SECONDS.toNanos(3)));
		assertDoubleEquals((1 + 2 + 3) / (3 / 1.5), rs.rate(TimeUnit.SECONDS.toNanos(3), Duration.ofMillis(1500)));
		rs.tick(-2, TimeUnit.SECONDS.toNanos(5));
		rs.tick(1, TimeUnit.SECONDS.toNanos(3));
		rs.tick(-1, TimeUnit.SECONDS.toNanos(6));
		assertDoubleEquals((-2 - 1), rs.rate(TimeUnit.SECONDS.toNanos(7)));
		assertDoubleEquals((-2 - 1) / (3 / 1.5), rs.rate(TimeUnit.SECONDS.toNanos(7), Duration.ofMillis(1500)));
	}

	private static final void assertDoubleEquals(final double extected, final double actual) {
		assertTrue(String.format("Expected %s but was %s", extected, actual),
				Math.abs(extected - actual) <= 0.0000000000001);
	}
}