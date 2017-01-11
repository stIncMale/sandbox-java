package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import java.util.function.Supplier;
import org.junit.Test;
import static java.time.Duration.ofNanos;
import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public abstract class AbstractRateMeterUnitTest<B extends RateMeterConfig.Builder, C extends RateMeterConfig> extends AbstractRateMeterTest<B, C> {
  AbstractRateMeterUnitTest(final Supplier<B> rateMeterConfigBuilderSupplier, final RateMeterCreator<C> rateMeterCreator) {
    super(rateMeterConfigBuilderSupplier, rateMeterCreator);
  }

  @Test
  public final void create1() {
    newRateMeter(0, ofNanos(10), ofNanos(10));
  }

  @Test(expected = IllegalArgumentException.class)
  public final void create2() {
    newRateMeter(0, ofNanos(10), ofNanos(11));
  }

  @Test
  public final void getStartNanos() {
    final long startNanos = 123;
    assertEquals(startNanos, newRateMeter(startNanos, ofNanos(10)).getStartNanos());
  }

  @Test
  public final void getSamplesInterval() {
    final Duration samplesInterval = ofNanos(10);
    assertEquals(samplesInterval, newRateMeter(0, samplesInterval).getSamplesInterval());
  }

  @Test
  public final void rightSamplesWindowBoundary1() {
    final long startNanos = 123;
    assertEquals(startNanos, newRateMeter(startNanos, ofNanos(10)).rightSamplesWindowBoundary());
  }

  @Test
  public final void rightSamplesWindowBoundary2() {
    final RateMeter rm = newRateMeter(0, ofNanos(10));
    final long rightmost = 123;
    rm.tick(1, rightmost);
    assertEquals(rightmost, rm.rightSamplesWindowBoundary());
  }

  @Test
  public final void rightSamplesWindowBoundary3() {
    final RateMeter rm = newRateMeter(0, ofNanos(10), ofNanos(7));
    final long rightmost = 123;
    rm.tick(1, rightmost);
    assertEquals(rightmost, rm.rightSamplesWindowBoundary(), 7);
  }

  @Test
  public final void ticksCount1() {
    assertEquals(0, newRateMeter(0, ofNanos(1)).ticksCount());
    assertReading(0, 0, true, newRateMeter(0, ofNanos(1)).ticksCount(new RateMeterReading()));
  }

  @Test
  public final void ticksCount2() {
    final RateMeterReading r = new RateMeterReading();
    final RateMeter rm = newRateMeter(-5, ofNanos(50));
    rm.tick(1, 7);
    rm.tick(4, 10);
    rm.tick(2, 20);
    rm.tick(3, 30);
    assertEquals(1 + 4 + 2 + 3, rm.ticksCount());
    assertReading(rm.ticksCount(), 30, true, rm.ticksCount(r));
    rm.tick(0, 8);
    assertEquals(1 + 4 + 2 + 3, rm.ticksCount());
    assertReading(rm.ticksCount(), 30, true, rm.ticksCount(r));
    rm.tick(-2, 50);
    rm.tick(1, 30);
    rm.tick(-1, 60);
    rm.tick(5, 10);
    assertEquals(2 + 3 - 2 + 1 - 1, rm.ticksCount());
    assertReading(rm.ticksCount(), 60, true, rm.ticksCount(r));
  }

  @Test
  public final void ticksTotalCount1() {
    assertEquals(0, newRateMeter(0, ofNanos(10)).ticksTotalCount());
  }

  @Test
  public final void ticksTotalCount2() {
    final RateMeter rm = newRateMeter(Long.MAX_VALUE, ofNanos(50));
    rm.tick(1, Long.MIN_VALUE);
    assertEquals(1, rm.ticksTotalCount());
    assertEquals(rm.ticksCount(), rm.ticksTotalCount());
    assertReading(rm.ticksCount(), Long.MIN_VALUE, true, rm.ticksCount(new RateMeterReading()));
    rm.tick(4, -210);
    rm.tick(2, -202);
    assertEquals(1 + 4 + 2, rm.ticksTotalCount());
  }

  @Test(expected = IllegalArgumentException.class)
  public final void tick1() {
    newRateMeter(-1, ofNanos(10))
        .tick(-1, Long.MAX_VALUE);
  }

  @Test(expected = IllegalArgumentException.class)
  public final void tick2() {
    newRateMeter(0, ofNanos(10))
        .tick(0, Long.MIN_VALUE);
  }

  @Test(expected = IllegalArgumentException.class)
  public final void tick3() {
    newRateMeter(1, ofNanos(10))
        .tick(1, 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public final void tick4() {
    newRateMeter(Long.MAX_VALUE, ofNanos(1))
        .tick(1, -1);
  }

  @Test
  public final void tick5() {
    newRateMeter(Long.MAX_VALUE, ofNanos(1))
        .tick(1, -6);
  }

  @Test
  public final void tick6() {
    newRateMeter(0, ofNanos(10))
        .tick(1, 1);
  }

  @Test
  public final void rateAverage1() {
    assertDoubleEquals(0, newRateMeter(0, ofNanos(10)).rateAverage());
  }

  @Test
  public final void rateAverage2() {
    assertDoubleEquals(0, newRateMeter(0, ofNanos(10)).rateAverage(0));
  }

  @Test
  public final void rateAverage3() {
    final RateMeter rm = newRateMeter(0, ofNanos(50));
    rm.tick(1, 7);
    rm.tick(1, 10);
    rm.tick(2, 20);
    rm.tick(0, 20);
    rm.tick(3, 30);
    rm.tick(-2, 50);
    rm.tick(1, 30);
    rm.tick(-1, 60);
    rm.tick(4, 1);
    assertDoubleEquals((1d + 1 + 2 + 3 - 2 + 1 - 1 + 4) / (60d / 50), rm.rateAverage());
    assertDoubleEquals((1d + 1 + 2 + 3 - 2 + 1 - 1 + 4) / (60d / 20), rm.rateAverage(ofNanos(20)));
    rm.tick(1000, 100);
    assertDoubleEquals((1d + 1 + 2 + 3 - 2 + 1 - 1 + 4 + 1000) / (100d / 50), rm.rateAverage());
    assertDoubleEquals((1d + 1 + 2 + 3 - 2 + 1 - 1 + 4 + 1000) / (100d / 20), rm.rateAverage(ofNanos(20)));
    assertDoubleEquals(rm.rateAverage(rm.rightSamplesWindowBoundary()), rm.rateAverage());
    assertDoubleEquals(rm.rateAverage(rm.rightSamplesWindowBoundary(), ofNanos(20)), rm.rateAverage(ofNanos(20)));
  }

  @Test
  public final void rateAverage4() {
    final RateMeter rm = newRateMeter(0, ofNanos(50));
    rm.tick(4, 0);
    rm.tick(1, 3);
    rm.tick(0, 10);
    rm.tick(2, 20);
    assertDoubleEquals((4d + 1 + 2) / (20d / 50), rm.rateAverage(20));
    assertDoubleEquals((4d + 1 + 2) / (20d / 30), rm.rateAverage(20, ofNanos(30)));
    rm.tick(3, 30);
    rm.tick(-2, 50);
    rm.tick(1, 30);
    assertDoubleEquals(4d + 1 + 2 + 3 - 2 + 1, rm.rateAverage(50));
    rm.tick(-1, 60);
    assertDoubleEquals((4d + 1 + 2 + 3 - 2 + 1 - 1) / (100d / 50), rm.rateAverage(100));
    assertDoubleEquals((4d + 1 + 2 + 3 - 2 + 1 - 1) / (100d / 30), rm.rateAverage(100, ofNanos(30)));
    assertDoubleEquals((4 + 1 + 2 + 3 - 2 + 1 - 1) / (60d / 50), rm.rateAverage(60));
    assertDoubleEquals(rm.rateAverage(rm.rightSamplesWindowBoundary()), rm.rateAverage(30));
    assertDoubleEquals(rm.rateAverage(), rm.rateAverage(10));
  }

  @Test
  public final void rate1() {
    assertDoubleEquals(0, newRateMeter(0, ofNanos(10)).rate());
    assertReading(0, 0, true, newRateMeter(0, ofNanos(10)).rate(new RateMeterReading()));
  }

  @Test
  public final void rate2() {
    assertDoubleEquals(0, newRateMeter(0, ofNanos(10)).rate(0));
    assertReading(0, 0, true, newRateMeter(0, ofNanos(10)).rate(0, new RateMeterReading()));
  }

  @Test
  public final void rate3() {
    final RateMeterReading r = new RateMeterReading();
    final RateMeter rm = newRateMeter(-1, ofNanos(30));
    rm.tick(1, 0);
    rm.tick(1, 5);
    assertDoubleEquals(1d + 1, rm.rate());
    assertReading(rm.rate(), 5, true, rm.rate(r));
    assertDoubleEquals((1d + 1) / (30d / 10), rm.rate(ofNanos(10)));
    assertReading(rm.rate(ofNanos(10)), 5, true, rm.rate(ofNanos(10), r));
    rm.tick(0, 10);
    rm.tick(2, 20);
    assertDoubleEquals(1 + 1 + 2, rm.rate());
    assertReading(rm.rate(), 20, true, rm.rate(r));
    assertDoubleEquals((1d + 1 + 2) / (30d / 10), rm.rate(ofNanos(10)));
    assertReading(rm.rate(ofNanos(10)), 20, true, rm.rate(ofNanos(10), r));
    rm.tick(3, 30);
    assertDoubleEquals(1 + 2 + 3, rm.rate());
    assertReading(rm.rate(), 30, true, rm.rate(r));
    rm.tick(-2, 50);
    rm.tick(1, 30);
    rm.tick(-1, 60);
    assertDoubleEquals(-2 - 1, rm.rate());
    assertReading(rm.rate(), 60, true, rm.rate(r));
    assertDoubleEquals((-2d - 1) / (30d / 10), rm.rate(ofNanos(10)));
    assertReading(rm.rate(ofNanos(10)), 60, true, rm.rate(ofNanos(10), r));
    assertDoubleEquals(rm.ticksCount(), rm.rate());
    assertReading(rm.ticksCount(), 60, true, rm.rate(r));
    assertReading(rm.rate(), 60, true, rm.ticksCount(new RateMeterReading()));
    assertDoubleEquals(rm.rate(rm.rightSamplesWindowBoundary()), rm.rate());
    assertDoubleEquals(rm.rate(rm.rightSamplesWindowBoundary(), ofNanos(10)), rm.rate(ofNanos(10)));
    assertReading(rm.rate(ofNanos(10)), 60, true, rm.rate(ofNanos(10), r));
    assertReading(rm.rate(5), 60, false, rm.rate(5, r));
  }

  @Test
  public final void rate4() {
    final RateMeterReading r = new RateMeterReading();
    final RateMeter rm = newRateMeter(-2, ofNanos(3));
    rm.tick(3, 0);
    rm.tick(1, 1);
    rm.tick(0, 1);
    rm.tick(2, 2);
    assertDoubleEquals((3d + 1 + 2), rm.rate(2));
    assertReading(rm.rate(2), 2, true, rm.rate(2, r));
    assertReading(rm.rateAverage(), 2, false, rm.rate(-2, r));
    assertDoubleEquals((3d + 1 + 2) / (3d / 17), rm.rate(2, ofNanos(17)));
    assertReading(rm.rate(2, ofNanos(17)), 2, true, rm.rate(2, ofNanos(17), r));
    rm.tick(3, 3);
    assertDoubleEquals((1 + 2 + 3), rm.rate(3));
    assertReading(rm.rate(3), 3, true, rm.rate(3, r));
    assertDoubleEquals((1d + 2 + 3) / (3d / 15), rm.rate(3, ofNanos(15)));
    assertReading(rm.rate(3, ofNanos(15)), 3, true, rm.rate(3, ofNanos(15), r));
    rm.tick(-2, 5);
    rm.tick(1, 3);
    rm.tick(-1, 6);
    assertDoubleEquals((-2 - 1), rm.rate(7));
    assertReading(rm.rate(7), 7, true, rm.rate(7, r));
    assertDoubleEquals((-2d - 1) / (3d / 15), rm.rate(7, ofNanos(15)));
    assertReading(rm.rate(7, ofNanos(15)), 7, true, rm.rate(7, ofNanos(15), r));
    assertDoubleEquals((2d + 3 + 1), rm.rate(4));
    assertReading(rm.rate(4), 4, true, rm.rate(4, r));
  }

  @Test
  public final void rate5() {
    final RateMeterReading r = new RateMeterReading();
    final RateMeter rm = newRateMeter(-2, ofNanos(30));
    rm.tick(1, -1);
    rm.tick(3, 0);
    rm.tick(1, 10);
    rm.tick(0, 10);
    rm.tick(2, 20);
    assertDoubleEquals((1d + 3 + 1 + 2), rm.rate(20));
    assertReading(rm.rate(20), 20, true, rm.rate(20, r));
    assertReading(0, 50, true, rm.rate(50, r));
    assertDoubleEquals((1d + 3 + 1 + 2) / (30d / 17), rm.rate(20, ofNanos(17)));
    assertReading(rm.rate(20, ofNanos(17)), 20, true, rm.rate(20, ofNanos(17), r));
    rm.tick(3, 30);
    assertDoubleEquals((1 + 2 + 3), rm.rate(30));
    assertReading(rm.rate(30), 30, true, rm.rate(30, r));
    assertDoubleEquals((1d + 2 + 3) / (30d / 15), rm.rate(30, ofNanos(15)));
    assertReading(rm.rate(30, ofNanos(15)), 30, true, rm.rate(30, ofNanos(15), r));
    rm.tick(-2, 50);
    rm.tick(1, 30);
    rm.tick(-1, 60);
    assertDoubleEquals((-2 - 1), rm.rate(70));
    assertReading(rm.rate(70), 70, true, rm.rate(70, r));
    assertReading(rm.rateAverage(), 60, false, rm.rate(30, r));
    assertDoubleEquals((-2d - 1) / (30d / 15), rm.rate(70, ofNanos(15)));
    assertReading(rm.rate(70, ofNanos(15)), 70, true, rm.rate(70, ofNanos(15), r));
  }

  private final RateMeter newRateMeter(final long startNanos, final Duration samplesInterval) {
    return newRateMeter(startNanos, samplesInterval, ofNanos(1));
  }

  private final RateMeter newRateMeter(final long startNanos, final Duration samplesInterval, final Duration timeSensitivity) {
    @SuppressWarnings("unchecked")
    final C rateMeterConfig = (C)getRateMeterConfigBuilderSupplier()
        .get()
        .setCheckArguments(true)
        .setTimeSensitivity(timeSensitivity)
        .build();
    return getRateMeterCreator().create(
        startNanos,
        samplesInterval,
        rateMeterConfig);
  }

  private static final void assertDoubleEquals(final double extected, final double actual) {
    assertEquals(extected, actual, 0.000000000001);
  }

  private static final void assertReading(final long expectedValue, final long expectedTNanos, final boolean accurate, final RateMeterReading reading) {
    assertEquals(expectedValue, reading.getLongValue());
    assertEquals(expectedTNanos, reading.getTNanos());
    if (accurate) {
      assertTrue(reading.isAccurate());
    } else {
      assertFalse(reading.isAccurate());
    }
  }

  private static final void assertReading(final double expectedValue, final long expectedTNanos, final boolean accurate, final RateMeterReading reading) {
    assertDoubleEquals(expectedValue, reading.getDoubleValue());
    assertEquals(expectedTNanos, reading.getTNanos());
    if (accurate) {
      assertTrue(reading.isAccurate());
    } else {
      assertFalse(reading.isAccurate());
    }
  }
}