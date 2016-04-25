package stinc.male.sandbox.ratexecutor;

import java.util.Comparator;
import javax.annotation.Nonnull;
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
	 * @return
	 * An instance of {@link NanosComparator}.
	 */
	public static final NanosComparator getInstance() {
		return instance;
	}

	/**
	 * Acts like {@link #compare(Long, Long)} but for primitive values.
	 * @throws IllegalArgumentException
	 * If {@code l1} - {@code l2} == {@link Long#MIN_VALUE}.
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
	 * @throws IllegalArgumentException
	 * If {@code l1} - {@code l2} == {@link Long#MIN_VALUE}.
	 */
	@Override
	public final int compare(@Nonnull final Long l1, @Nonnull final Long l2) throws IllegalArgumentException {
		checkNotNull(l1, "o1");
		checkNotNull(l2, "o2");
		return compare(l1.longValue(), l2.longValue());
	}

	private NanosComparator() {
	}
}