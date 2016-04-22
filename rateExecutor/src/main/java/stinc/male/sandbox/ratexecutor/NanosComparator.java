package stinc.male.sandbox.ratexecutor;

import java.util.Comparator;
import javax.annotation.concurrent.ThreadSafe;
import static stinc.male.sandbox.ratexecutor.Preconditions.checkNotNull;

@ThreadSafe
final class NanosComparator implements Comparator<Long> {
	private static final NanosComparator instance = new NanosComparator();

	static final NanosComparator getInstance() {
		return instance;
	}

	static final int compare(long l1, long l2) {
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

	private NanosComparator() {
	}

	@Override
	public final int compare(final Long o1, final Long o2) {
		checkNotNull(o1, "o1");
		checkNotNull(o2, "o2");
		return compare(o1.longValue(), o2.longValue());
	}
}