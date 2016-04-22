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
			new Sample(-1, Long.MIN_VALUE, CompareResult.GREATER),//maxDelta
			new Sample(Long.MAX_VALUE - Long.MAX_VALUE / 2, Long.MIN_VALUE + Long.MAX_VALUE / 2, CompareResult.LOWER),//maxDelta
			new Sample(Long.MIN_VALUE, Long.MAX_VALUE, CompareResult.GREATER),
			new Sample(Long.MIN_VALUE + 1000, Long.MAX_VALUE - 1000, CompareResult.GREATER),
			new Sample(-2, 5, CompareResult.LOWER),
			new Sample(5, 3, CompareResult.GREATER),
	};

	public NanosComparatorTest() {
	}

	@Test
	public final void compare() {
		for (final Sample sample : samples) {
			assertCompare(sample.l1, sample.l2, sample.expected);
		}
	}

	private static final void enshureCompatibility(final long l1, final long l2) {
		long delta = l1 -l2;
		assertTrue(String.format("l1 = %s, l2 = %s", l1, l2), delta != Long.MIN_VALUE);//Math.abs can't handle this value
		assertTrue(String.format("l1 = %s, l2 = %s", l1, l2), Math.abs(delta) <= maxDelta);
	}

	private static final void unsafeAssertCompareSymmetry(final long l1, final long l2) {
		assertSame(String.format("l1 = %s, l2 = %s", l1, l2),
				Integer.signum(NanosComparator.compare(l1, l2)), -Integer.signum(NanosComparator.compare(l2, l1)));
	}

	private static final void assertCompare(final long l1, final long l2, final CompareResult expected) {
		enshureCompatibility(l1, l2);
		unsafeAssertCompareSymmetry(l1, l2);
		switch (expected) {
			case LOWER: {
				assertTrue(String.format("l1 = %s, l2 = %s", l1, l2), NanosComparator.compare(l1, l2) < 0);
				break;
			}
			case GREATER: {
				assertTrue(String.format("l1 = %s, l2 = %s", l1, l2), NanosComparator.compare(l1, l2) > 0);
				break;
			}
			case EQUAL: {
				assertTrue(String.format("l1 = %s, l2 = %s", l1, l2), NanosComparator.compare(l1, l2) == 0);
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
		EQUAL;
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