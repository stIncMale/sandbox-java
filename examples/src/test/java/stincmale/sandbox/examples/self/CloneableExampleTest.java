package stincmale.sandbox.examples.self;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.Test;

final class CloneableExampleTest {
    private CloneableExampleTest() {
    }

    @Test
    final void copy() {
        final CloneableExample orig = new CloneableExample("value");
        final CloneableExample clone = orig.clone();
        assertNotSame(orig, clone);
        assertSame(orig.getClass(), clone.getClass());
        assertEquals(orig, clone);
    }
}
