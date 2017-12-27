package stinc.male.sandbox.ratmex.executor;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RateTest {
    public RateTest() {
    }

    @Test
    public final void toUnit1() {
        final Rate r1 = new Rate(-1, 1, Duration.ofSeconds(1));
        final Rate r2 = r1.toUnit(Duration.ofMillis(50));
        assertEquals(r1.getMin() / 20, r2.getMin());
        assertEquals(r1.getMax() / 20, r2.getMax());
    }

    @Test
    public final void toUnit2() {
        final Rate r1 = new Rate(-11, 31, Duration.ofSeconds(1));
        final Rate r2 = r1.toUnit(Duration.ofHours(1));
        assertEquals(r1.getMin() * 3600, r2.getMin());
        assertEquals(r1.getMax() * 3600, r2.getMax());
    }
}