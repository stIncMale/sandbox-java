package stinc.male.sandbox.ratexecutor;

import java.math.BigInteger;
import org.junit.Test;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public final class NanosComparatorTest {
	private static final long maxDelta = BigInteger.valueOf(2).pow(63).subtract(BigInteger.ONE).longValueExact();
	private static final Sample[] samples = {
			new Sample(Long.MIN_VALUE, Long.MIN_VALUE, CompareResult.EQUAL),
			new Sample(Long.MAX_VALUE, Long.MAX_VALUE, CompareResult.EQUAL),
			new Sample(0, 0, CompareResult.EQUAL),
			new Sample(0, Long.MAX_VALUE, CompareResult.LOWER),//maxDelta
			new Sample(-2, Long.MAX_VALUE, CompareResult.GREATER),//maxDelta + 1
			new Sample(0, Long.MAX_VALUE, CompareResult.LOWER),//maxDelta
			new Sample(-1, Long.MIN_VALUE, CompareResult.GREATER),//maxDelta
			new Sample(Long.MAX_VALUE - Long.MAX_VALUE / 2, Long.MIN_VALUE + Long.MAX_VALUE / 2, CompareResult.LOWER),//maxDelta
			new Sample(Long.MIN_VALUE, Long.MAX_VALUE, CompareResult.GREATER),
			new Sample(Long.MIN_VALUE + 1000, Long.MAX_VALUE - 1000, CompareResult.GREATER),
			new Sample(-2, 5, CompareResult.LOWER),
			new Sample(5, 3, CompareResult.GREATER),
	};

	private static final Sample[] incompatibleSamples = {
			new Sample(-1, Long.MAX_VALUE, CompareResult.UNSUPPORTED),//maxDelta + 1
//			new Sample(-1, Long.MIN_VALUE, CompareResult.UNSUPPORTED),//maxDelta
//			new Sample(Long.MAX_VALUE - Long.MAX_VALUE / 2, Long.MIN_VALUE + Long.MAX_VALUE / 2, CompareResult.UNSUPPORTED),//maxDelta
//			new Sample(Long.MIN_VALUE, Long.MAX_VALUE, CompareResult.UNSUPPORTED),
//			new Sample(Long.MIN_VALUE + 1000, Long.MAX_VALUE - 1000, CompareResult.UNSUPPORTED),
//			new Sample(-2, 5, CompareResult.UNSUPPORTED),
//			new Sample(5, 3, CompareResult.UNSUPPORTED),
	};

	public NanosComparatorTest() {
	}

	@Test
	public final void compareUnsafe() {
		for (final Sample sample : samples) {
			assertCompare(sample.l1, sample.l2, sample.expected, NanosComparator.getUnsafeInstance());
		}
	}

	@Test
	public final void compare() {
		for (final Sample sample : incompatibleSamples) {
			assertCompare(sample.l1, sample.l2, sample.expected, NanosComparator.getInstance());
		}
	}

	private static final void unsafeAssertCompareSymmetry(final Long l1, final Long l2, final NanosComparator comparator) {
		assertSame(String.format("l1 = %s, l2 = %s", l1, l2),
				Integer.signum(comparator.compare(l1, l2)), -Integer.signum(comparator.compare(l2, l1)));
	}

	private static final void assertCompare(final Long l1, final Long l2, final CompareResult expected, final NanosComparator comparator) {
		if (expected != CompareResult.UNSUPPORTED) {
			unsafeAssertCompareSymmetry(l1, l2, comparator);
		}
		switch (expected) {
			case LOWER: {
				assertTrue(String.format("l1 = %s, l2 = %s", l1, l2), comparator.compare(l1, l2) < 0);
				break;
			}
			case GREATER: {
				assertTrue(String.format("l1 = %s, l2 = %s", l1, l2), comparator.compare(l1, l2) > 0);
				break;
			}
			case EQUAL: {
				assertTrue(String.format("l1 = %s, l2 = %s", l1, l2), comparator.compare(l1, l2) == 0);
				break;
			}
			case UNSUPPORTED: {
				boolean illegalArguments = false;
				try {
					comparator.compare(l1, l2);
				} catch (final IllegalArgumentException e) {
					illegalArguments = true;
				}
				assertTrue(String.format("l1 = %s, l2 = %s, illegalArguments = %s, comparator = %s", l1, l2, illegalArguments, comparator),
						illegalArguments ^ (comparator == NanosComparator.getUnsafeInstance()));//use equals
				break;
			}
			default: {
				throw new RuntimeException();
			}
		}
	}

	private enum CompareResult {
		LOWER,
		GREATER,
		EQUAL,
		UNSUPPORTED;
	}

	private static final class Sample {
		final long l1;
		final long l2;
		final CompareResult expected;

		Sample(final long l1, final long l2, final CompareResult expected) {
			this.l1 = l1;
			this.l2 = l2;
			this.expected = expected;
		}
	}
}