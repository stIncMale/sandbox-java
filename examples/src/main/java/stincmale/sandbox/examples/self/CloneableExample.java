package stincmale.sandbox.examples.self;

import java.util.Objects;

final class CloneableExample implements Cloneable {
    private final String value;

    CloneableExample(final String value) {
        this.value = value;
    }

    // @CheckstyleOff NoClone
    @Override
    public final CloneableExample clone() {
        final CloneableExample clone;
        try {
            clone = (CloneableExample) super.clone();
            return clone;
        } catch (final CloneNotSupportedException e) {
            throw new AssertionError("Unreachable");
        }
    }
    // @CheckstyleOn NoClone

    @Override
    public final boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final CloneableExample that = (CloneableExample) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(value);
    }
}
