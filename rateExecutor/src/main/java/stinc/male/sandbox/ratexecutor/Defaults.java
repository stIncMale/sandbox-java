package stinc.male.sandbox.ratexecutor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Specifies rules that are applied to a package.
 * <p>
 * The key words "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT", "SHOULD", "SHOULD NOT", "RECOMMENDED", "MAY", and
 * "OPTIONAL" here and in all packages annotated with this annotations are to be interpreted as described in <a
 * href="http://www.ietf.org/rfc/rfc2119.txt">RFC 2119</a>.
 * <p>
 * Default properties of all classes, interfaces, methods and constructors in the package:
 * <ol>
 * <li>Classes SHOULD be treated mutable and not thread-safe unless otherwise is explicitly specified ({@link Immutable}
 * and {@link ThreadSafe} annotation SHOULD be used for this purpose).</li>
 * <li>Arguments and return values of methods MUST be non-{@code null} unless otherwise is explicitly specified. It
 * SHOULD be specified by the {@link ParametersAreNonnullByDefault} annotation on package/class and/or by {@link Nullable}/
 * {@link Nonnull} annotations on fields/local variables/methods.</li>
 * <li>Methods and constructors MUST NOT modify modifiable<b><sup>*</sup></b> objects passed as arguments unless
 * otherwise is explicitly specified.</li>
 * <li>Modifications of modifiable objects returned from methods MUST NOT affect the state of the object on which the
 * method is invoked unless otherwise is explicitly specified. E.g. modification of contents of a {@link List} returned
 * from {@code someObject.getList()} method MUST NOT affect state of the {@code someObject}.</li>
 * <li>If a method returns an object that is supposed to be modifiable the object MUST be modifiable unless otherwise is
 * explicitly specified.</li>
 * <li>{@link Enumeration}, {@link Iterable}, {@link Stream}, array arguments and return values MUST NOT contain {@code null}
 * elements unless otherwise is explicitly specified.</li>
 * <li>{@link Enumeration}, {@link Iterable}, {@link Stream}, array arguments and return values SHOULD NOT contain duplicate
 * elements (see {@link Object#equals(Object)}) unless otherwise is explicitly specified. In some obvious cases an explicit
 * specification MAY be omitted (for example in case of a {@code byte[]}).</li>
 * <li>Any timeouts MUST be interpreted as approximate periods of real time.
 * See {@link Object#wait(long)} for that "more or less" phrase.</li>
 * <li>One SHOULD remember that format of {@link String}s returned by any method that overrides {@link Object#toString()}
 * MAY be changed at any time unless the format is explicitly specified in the method specification.</li>
 * </ol>
 * <p>
 * <b><sup>*</sup></b> Here and in any package annotated with {@link Defaults} we distinguish immutable and unmodifiable
 * Java objects. <i>Unmodifiable object</i> (aka <i>read-only object</i>) is an object that is not intended to be
 * modified via its API (<i>modifiable object</i> is the opposite), but it still MAY change its observable state. E.g.
 * object returned by {@link Collections#unmodifiableCollection(java.util.Collection)} can't be modified via it's API,
 * but because such an object is just a view of an actual {@link Collection}, its observable state MAY be modified.
 * <i>Immutable object</i> is an object which observable state can't be changed once you have a reference to the object.
 * Observable state of an immutable object can't even be changed because of publication via data race), and because of
 * this immutable objects are inherently thread-safe just as specified by {@link Immutable} annotation.
 */
//TODO read, check, correct; remove javax imports
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.PACKAGE})
public @interface Defaults {
}
