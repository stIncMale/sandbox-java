package stincmale.sandbox.examples.self;

/**
 * Constructs new instances when {@linkplain #append(String) appending}
 * instead of mutating {@code this}.
 *
 * @param <Self> A type introduced by the concrete class that extends this abstract class.
 */
abstract class AbstractCopyOnAppend<Self extends AbstractCopyOnAppend<Self>> {
    final String value;

    AbstractCopyOnAppend(final String value) {
        this.value = value;
    }

    final Self append(final String append) {
        return newSelf(value + append);
    }

    abstract Self newSelf(final String value);
}
