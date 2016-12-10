package stinc.male.sandbox.ratexecutor;

import java.util.Comparator;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import static stinc.male.sandbox.ratexecutor.Preconditions.checkNotNull;

/**
 * A {@linkplain Comparator comparator} that compares nanoseconds according to {@link System#nanoTime()} specification.
 */
@ThreadSafe
public final class NanosComparator implements Comparator<Long> {
  private static final NanosComparator instance = new NanosComparator();

  /**
   * Always returns the same instance.
   *
   * @return An instance of {@link NanosComparator}.
   */
  public static final NanosComparator instance() {
    return instance;
  }

  /**
   * Acts like {@link #compare(Long, Long)} but for primitive values.
   *
   * @throws IllegalArgumentException If {@code l1} - {@code l2} == {@link Long#MIN_VALUE}.
   */
  public static final int compare(final long l1, final long l2) throws IllegalArgumentException {
    final long diff = l1 - l2;
    if (diff == Long.MIN_VALUE) {
      throw new IllegalArgumentException(String.format("Nanos %s and %s aren't comparable", l1, l2));
    }
    final int result;
    if (diff < 0) {
      result = -1;
    } else if (diff > 0) {
      result = 1;
    } else {
      result = 0;
    }
    return result;
  }

  /**
   * Returns the smaller (see {@link #compare(long, long)}) of two nanosecond values.
   * If the arguments have the same value, the result is that same value.
   *
   * @param l1 An argument.
   * @param l2 Another argument.
   * @return The smaller of two nanosecond values.
   * @throws IllegalArgumentException If {@code l1} - {@code l2} == {@link Long#MIN_VALUE}.
   */
  public static final long min(final long l1, final long l2) throws IllegalArgumentException {
    return compare(l1, l2) <= 0 ? l1 : l2;
  }

  /**
   * Returns the greater (see {@link #compare(long, long)}) of two nanosecond values.
   * If the arguments have the same value, the result is that same value.
   *
   * @param l1 An argument.
   * @param l2 Another argument.
   * @return The larger of two nanosecond values.
   * @throws IllegalArgumentException If {@code l1} - {@code l2} == {@link Long#MIN_VALUE}.
   */
  public static final long max(final long l1, final long l2) throws IllegalArgumentException {
    return compare(l1, l2) >= 0 ? l1 : l2;
  }

  /**
   * @throws IllegalArgumentException If {@code l1} - {@code l2} == {@link Long#MIN_VALUE}.
   */
  @Override
  public final int compare(@Nonnull final Long l1, @Nonnull final Long l2) throws IllegalArgumentException {
    checkNotNull(l1, "o1");
    checkNotNull(l2, "o2");
    return compare(l1.longValue(), l2.longValue());
  }

  /**
   * @return {@code true} if and only if {@code obj} is instance of {@link NanosComparator}.
   */
  @Override
  public final boolean equals(@Nullable final Object obj) {
    return obj instanceof NanosComparator;
  }

  @Override
  public final int hashCode() {
    return 2069133341;
  }

  private NanosComparator() {
  }
}