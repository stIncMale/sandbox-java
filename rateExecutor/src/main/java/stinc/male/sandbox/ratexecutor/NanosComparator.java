package stinc.male.sandbox.ratexecutor;

import java.math.BigInteger;
import java.util.Comparator;
import javax.annotation.concurrent.ThreadSafe;
import static stinc.male.sandbox.ratexecutor.Preconditions.checkNotNull;

@ThreadSafe
final class NanosComparator implements Comparator<Long> {
	private static final long MAX_DIFF = BigInteger.valueOf(2).pow(63).subtract(BigInteger.ONE).longValueExact();
	private static final NanosComparator unsafeInstance = new NanosComparator(false);
	private static final NanosComparator instance = new NanosComparator(true);

	public static final NanosComparator getInstance() {
		return instance;
	}

	public static final NanosComparator getUnsafeInstance() {
		return unsafeInstance;
	}

	private final boolean safe;

	public static final int compareUnsafe(final long l1, final long l2) {
		final long diff = l1 - l2;
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

	public static final int compare(final long l1, final long l2) throws IllegalArgumentException {
		checkArguments(l1, l2);
		return compareUnsafe(l1, l2);
	}

	@Override
	public final int compare(final Long o1, final Long o2) {
		checkNotNull(o1, "o1");
		checkNotNull(o2, "o2");
		return safe
				? compare(o1.longValue(), o2.longValue())
				: compareUnsafe(o1.longValue(), o2.longValue());
	}

	@Override
	public final String toString() {
		return getClass().getSimpleName() + "(safe=" + safe + ')';
	}

	private static final void checkArguments(final long l1, final long l2) throws IllegalArgumentException {
		final long diff12 = l1 - l2;
		if (Math.abs(diff12) > MAX_DIFF || diff12 == Long.MIN_VALUE) {//Math.abs can't handle Long.MIN_VALUE
			throw new IllegalArgumentException(
					String.format("Difference between %s and %s has greater absolute value than %s", l1, l2, MAX_DIFF));
		}
		final long diff21 = l2 - l1;
		if (Math.abs(diff21) > MAX_DIFF || diff21 == Long.MIN_VALUE) {//Math.abs can't handle Long.MIN_VALUE
			throw new IllegalArgumentException(
					String.format("Difference between %s and %s has greater absolute value than %s", l2, l1, MAX_DIFF));
		}
	}

	private NanosComparator(final boolean safe) {
		this.safe = safe;
	}
	//VAKOTODO hashCode and equals
	//VAKOTODO only check for diff == Long.MIN_VALUE because all other values are comparable
}