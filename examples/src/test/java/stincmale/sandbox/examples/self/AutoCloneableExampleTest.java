package stincmale.sandbox.examples.self;

import java.util.List;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.Test;

final class AutoCloneableExampleTest {
    @Test
    final void copy() {
        final AutoCloneableExample original = new AutoCloneableExample(List.of("a", "b"));
        final AutoCloneableExample copy = original.clone();
        assertNotSame(original, copy);
        assertSame(original.getClass(), copy.getClass());
        assertSame(original.value, copy.value);
    }

    private AutoCloneableExampleTest() {
    }
}
