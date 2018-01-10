package stinc.male.sandbox.ratmex.executor;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class RateTest {
  public RateTest() {
  }

  @Test
  public final void toUnit1() {
    final Rate r1 = new Rate(-1, 1, Duration.ofSeconds(1));
    final Rate r2 = r1.toUnit(Duration.ofMillis(50));
    assertEquals(r1.getMinValue() / 20, r2.getMinValue());
    assertEquals(r1.getMaxValue() / 20, r2.getMaxValue());
  }

  @Test
  public final void toUnit2() {
    final Rate r1 = new Rate(-11, 31, Duration.ofSeconds(1));
    final Rate r2 = r1.toUnit(Duration.ofHours(1));
    assertEquals(r1.getMinValue() * 3600, r2.getMinValue());
    assertEquals(r1.getMaxValue() * 3600, r2.getMaxValue());
  }

  @Test
  public final void testCompareTo() {
    final Rate r = new Rate(-1000, 1000_000, Duration.ofMillis(1));
    assertEquals(0, r.compareTo(-1000, r.getUnit()));
    assertEquals(0, r.compareTo(123456, r.getUnit()));
    assertEquals(0, r.compareTo(1000_000, r.getUnit()));
    assertEquals(1, r.compareTo(-1001, r.getUnit()));
    assertEquals(-1, r.compareTo(1000_001, r.getUnit()));
    assertEquals(0, r.compareTo(-1, Duration.of(1, ChronoUnit.MICROS)));
    assertEquals(0, r.compareTo(123.456, Duration.of(1, ChronoUnit.MICROS)));
    assertEquals(0, r.compareTo(1000, Duration.of(1, ChronoUnit.MICROS)));
    assertEquals(1, r.compareTo(-1.001, Duration.of(1, ChronoUnit.MICROS)));
    assertEquals(-1, r.compareTo(1000.001, Duration.of(1, ChronoUnit.MICROS)));
    assertEquals(0, r.compareTo(-1000_000, Duration.of(1, ChronoUnit.SECONDS)));
    assertEquals(0, r.compareTo(123456_000, Duration.of(1, ChronoUnit.SECONDS)));
    assertEquals(0, r.compareTo(1000_000_000, Duration.of(1, ChronoUnit.SECONDS)));
    assertEquals(1, r.compareTo(-1001_000, Duration.of(1, ChronoUnit.SECONDS)));
    assertEquals(-1, r.compareTo(1000_001_000, Duration.of(1, ChronoUnit.SECONDS)));
  }
}