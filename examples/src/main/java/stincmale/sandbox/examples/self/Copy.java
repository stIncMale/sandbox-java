package stincmale.sandbox.examples.self;

/**
 * Instances of subclasses of this class can be shallowly copied via {@link #clone()} without
 * the need to override the method.
 * <p>
 * Extending this class appears to be the closest counterpart we may have in pure Java to Rust's
 * <a href="https://doc.rust-lang.org/std/marker/trait.Copy.html#how-can-i-implement-copy">
 * `#[derive(Copy, Clone)]`</a>, which allows
 * <a href="https://doc.rust-lang.org/reference/attributes/derive.html">generating</a>
 * code that copies values.</p>
 *
 * @param <Self> A type introduced by the concrete subclass of this class.
 */
// @CheckstyleOff NoClone
abstract class Copy<Self extends Copy<Self>> implements Cloneable {
    protected Copy() {
    }

    @Override
    @SuppressWarnings("unchecked")
    public final Self clone() {
        try {
            return (Self) super.clone();
        } catch (final CloneNotSupportedException e) {
            throw new AssertionError("Unreachable");
        }
    }
}
// @CheckstyleOn NoClone
