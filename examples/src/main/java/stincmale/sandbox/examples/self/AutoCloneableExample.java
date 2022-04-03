package stincmale.sandbox.examples.self;

import java.util.List;

final class AutoCloneableExample extends Copy<AutoCloneableExample> {
    final List<String> value;

    AutoCloneableExample(final List<String> value) {
        this.value = value;
    }
}
