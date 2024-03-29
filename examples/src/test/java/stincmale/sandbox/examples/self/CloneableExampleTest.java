package stincmale.sandbox.examples.self;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.Test;

final class CloneableExampleTest {
    @Test
    final void copy() {
        final CloneableExample original = new CloneableExample("value");
        final CloneableExample copy = original.clone();
        assertNotSame(original, copy);
        assertSame(original.getClass(), copy.getClass());
        assertEquals(original.value, copy.value);
    }

    private CloneableExampleTest() {
    }
}
