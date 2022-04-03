package stincmale.sandbox.examples.self;

// @CheckstyleOff NoClone
final class CloneableExample implements Cloneable {
    final String value;

    CloneableExample(final String value) {
        this.value = value;
    }

    @Override
    public final CloneableExample clone() {
        try {
            return (CloneableExample) super.clone();
        } catch (final CloneNotSupportedException e) {
            throw new AssertionError("Unreachable");
        }
    }
}
// @CheckstyleOn NoClone
