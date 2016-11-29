package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public abstract class AbstractRateMeterTest {
  private final RateMeterCreator rateMeterCreator;

  AbstractRateMeterTest(final RateMeterCreator rateMeterCreator) {
    this.rateMeterCreator = rateMeterCreator;
  }

  @Test
  public final void getStartNanos() {
    final long startNanos = 123;
    assertEquals(startNanos, newRateMeter(startNanos, Duration.ofSeconds(1)).getStartNanos());
  }

  @Test
  public final void getSamplesInterval() {
    final Duration samplesInterval = Duration.ofSeconds(1);
    assertEquals(samplesInterval, newRateMeter(123, samplesInterval).getSamplesInterval());
  }

  @Test
  public final void rightSamplesWindowBoundary1() {
    final long startNanos = 123;
    assertEquals(startNanos, newRateMeter(startNanos, Duration.ofSeconds(1)).rightSamplesWindowBoundary());
  }

  @Test
  public final void rightSamplesWindowBoundary2() {
    final RateMeter rs = newRateMeter(0, Duration.ofSeconds(1));
    final long rightmost = 123;
    rs.tick(1, rightmost);
    assertEquals(rightmost, rs.rightSamplesWindowBoundary());
  }

  @Test
  public final void ticksCount1() {
    assertEquals(0, newRateMeter(0, Duration.ofSeconds(1)).ticksCount());
  }

  @Test
  public final void ticksCount2() {
    final RateMeter rs = newRateMeter(-5, Duration.ofSeconds(5));
    rs.tick(1, TimeUnit.SECONDS.toNanos(1) - 123);
    rs.tick(4, TimeUnit.SECONDS.toNanos(1));
    rs.tick(2, TimeUnit.SECONDS.toNanos(2));
    rs.tick(3, TimeUnit.SECONDS.toNanos(3));
    assertEquals(1 + 4 + 2 + 3, rs.ticksCount());
    rs.tick(0, TimeUnit.SECONDS.toNanos(8));//doesn't move a samples window
    assertEquals(1 + 4 + 2 + 3, rs.ticksCount());
    rs.tick(-2, TimeUnit.SECONDS.toNanos(5));
    rs.tick(1, TimeUnit.SECONDS.toNanos(3));
    rs.tick(-1, TimeUnit.SECONDS.toNanos(6));
    rs.tick(5, TimeUnit.SECONDS.toNanos(1));
    assertEquals(2 + 3 - 2 + 1 - 1, rs.ticksCount());
  }

  @Test
  public final void ticksTotalCount1() {
    assertEquals(0, newRateMeter(0, Duration.ofSeconds(1)).ticksTotalCount());
  }

  @Test
  public final void ticksTotalCount2() {
    final RateMeter rs = newRateMeter(Long.MAX_VALUE, Duration.ofSeconds(5));
    rs.tick(1, Long.MIN_VALUE);
    assertEquals(1, rs.ticksTotalCount());
    assertEquals(rs.ticksCount(), rs.ticksTotalCount());
    rs.tick(4, -TimeUnit.SECONDS.toNanos(6));
    rs.tick(2, -TimeUnit.SECONDS.toNanos(5) - 2);
    assertEquals(1 + 4 + 2, rs.ticksTotalCount());
  }

  @Test(expected = IllegalArgumentException.class)
  public final void tick1() {
    newRateMeter(-1, Duration.ofSeconds(1))
        .tick(-1, Long.MAX_VALUE);
  }

  @Test(expected = IllegalArgumentException.class)
  public final void tick2() {
    newRateMeter(0, Duration.ofSeconds(1))
        .tick(0, Long.MIN_VALUE);
  }

  @Test(expected = IllegalArgumentException.class)
  public final void tick3() {
    newRateMeter(1, Duration.ofSeconds(1))
        .tick(1, 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public final void tick4() {
    newRateMeter(Long.MAX_VALUE, Duration.ofNanos(1L))
        .tick(1, -1);
  }

  @Test
  public final void tick5() {
    newRateMeter(Long.MAX_VALUE, Duration.ofNanos(1L))
        .tick(1, -3);
  }

  @Test
  public final void tick6() {
    newRateMeter(0, Duration.ofSeconds(1))
        .tick(1, 1);
  }

  @Test
  public final void rateAverage1() {
    assertDoubleEquals(0, newRateMeter(0, Duration.ofSeconds(1)).rateAverage());
  }

  @Test
  public final void rateAverage2() {
    assertDoubleEquals(0, newRateMeter(0, Duration.ofSeconds(1)).rateAverage(0));
  }

  @Test
  public final void rateAverage3() {
    final RateMeter rs = newRateMeter(0, Duration.ofSeconds(5));
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
    assertDoubleEquals(rs.rateAverage(rs.rightSamplesWindowBoundary()), rs.rateAverage());
    assertDoubleEquals(rs.rateAverage(rs.rightSamplesWindowBoundary(), Duration.ofSeconds(2)), rs.rateAverage(Duration.ofSeconds(2)));
  }

  @Test
  public final void rateAverage4() {
    final RateMeter rs = newRateMeter(0, Duration.ofSeconds(5));
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
    assertDoubleEquals(0, newRateMeter(0, Duration.ofSeconds(1)).rate());
  }

  @Test
  public final void rate2() {
    assertDoubleEquals(0, newRateMeter(0, Duration.ofSeconds(1)).rate(0));
  }

  @Test
  public final void rate3() {
    final RateMeter rs = newRateMeter(TimeUnit.SECONDS.toNanos(-1), Duration.ofSeconds(3));
    rs.tick(1, 0);
    rs.tick(1, TimeUnit.SECONDS.toNanos(1) - 123);
    assertDoubleEquals(1 + 1, rs.rate());
    assertDoubleEquals((1d + 1) / (3 / 1), rs.rate(Duration.ofSeconds(1)));
    rs.tick(0, TimeUnit.SECONDS.toNanos(1));
    rs.tick(2, TimeUnit.SECONDS.toNanos(2));
    assertDoubleEquals(1 + 1 + 2, rs.rate());
    assertDoubleEquals((1d + 1 + 2) / (3 / 1), rs.rate(Duration.ofSeconds(1)));
    rs.tick(3, TimeUnit.SECONDS.toNanos(3));
    assertDoubleEquals(1 + 2 + 3, rs.rate());
    rs.tick(-2, TimeUnit.SECONDS.toNanos(5));
    rs.tick(1, TimeUnit.SECONDS.toNanos(3));
    rs.tick(-1, TimeUnit.SECONDS.toNanos(6));
    assertDoubleEquals(-2 - 1, rs.rate());
    assertDoubleEquals((-2d - 1) / (3 / 1), rs.rate(Duration.ofSeconds(1)));
    assertDoubleEquals(rs.ticksCount(), rs.rate());
    assertDoubleEquals(rs.rate(rs.rightSamplesWindowBoundary()), rs.rate());
    assertDoubleEquals(rs.rate(rs.rightSamplesWindowBoundary(), Duration.ofSeconds(1)), rs.rate(Duration.ofSeconds(1)));
  }

  @Test
  public final void rate4() {
    final RateMeter rs = newRateMeter(-2, Duration.ofSeconds(3));
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

  private final RateMeter newRateMeter(final long startNanos, final Duration samplesInterval) {
    return rateMeterCreator.create(
        startNanos,
        samplesInterval,
        RateMeterConfig.defaultInstance()
            .toBuilder()
            .setCheckTNanos(true)
            .build());
  }

  private static final void assertDoubleEquals(final double extected, final double actual) {
    assertEquals(extected, actual, 0.0000000000001);
  }

  interface RateMeterCreator {
    RateMeter create(long startNanos, Duration samplesInterval, RateMeterConfig config);
  }
}