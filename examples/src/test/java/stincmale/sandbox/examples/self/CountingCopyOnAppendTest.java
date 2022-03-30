package stincmale.sandbox.examples.self;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

final class CountingCopyOnAppendTest {
    private CountingCopyOnAppendTest() {
    }

    @Test
    final void append() {
        final CountingCopyOnAppend coa = new CountingCopyOnAppend("0")
                .append("1");
        assertEquals("01", coa.value);
        assertEquals(1, coa.appends);
    }
}
