package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import java.util.Comparator;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import static stinc.male.sandbox.ratexecutor.Preconditions.checkArgument;
import static stinc.male.sandbox.ratexecutor.Preconditions.checkNotNull;

@ThreadSafe
public final class ConcurrentRateSampler implements RateSampler {
	private final long startNanos;
	private final long sampleIntervalNanos;
	private AtomicLong aTotalCount;
	private final ConcurrentSkipListMap<Long, AtomicLong> samples;//(rightNanos - sampleIntervalNanos; rightNanos]

	public ConcurrentRateSampler(final long startNanos, final Duration sampleInterval) {
		checkArgument(startNanos != Double.NaN, "startNanos", "Must not be NaN");
		checkNotNull(sampleInterval, "sampleInterval");
		checkArgument(!sampleInterval.isZero(), "sampleInterval", "Must not be zero");
		checkArgument(!sampleInterval.isNegative(), "sampleInterval", "Must be positive");
		this.startNanos = startNanos;
		sampleIntervalNanos = sampleInterval.toNanos();
		aTotalCount = new AtomicLong();
		samples = new ConcurrentSkipListMap<>(NanosComparator.instance);
		samples.put(startNanos, new AtomicLong());
	}

	@Override
	public final synchronized void tick(final long count, final long tNanos) {
		checkArgument(count >= 0, "count", "Must not be negative");
		if (count > 0 && NanosComparator.compare(tNanos, startNanos) >= 0) {
			final AtomicLong newSample = new AtomicLong(count);
			@Nullable
			final AtomicLong existingSample = samples.putIfAbsent(tNanos, newSample);
			if (existingSample != null) {//we need to merge samples
				existingSample.addAndGet(count);
			}
			aTotalCount.addAndGet(count);
			gc();
		}
	}

	@Override
	public final synchronized double rateAverage() {
		return internalRateAverage(samples.lastKey());
	}

	@Override
	public final synchronized double rateAverage(final long tNanos) {
		return internalRateAverage(tNanos);
	}

	@Override
	public final synchronized double rate() {
		return internalRate(samples.lastKey());
	}

	@Override
	public final synchronized double rate(final long tNanos) {
		return internalRate(tNanos);
	}

	@Override
	public final synchronized String toString() {
		return getClass().getSimpleName()
				+ "(startNanos=" + startNanos
				+ ", sampleIntervalNanos" + sampleIntervalNanos
				+ ')';
	}

	private final synchronized double internalRateAverage(final long tNanos) {
		final double result;
		final long totalCount = aTotalCount.get();
		if (NanosComparator.compare(tNanos, startNanos) < 0 || totalCount == 0) {
			result = 0;
		} else {
			final long totalNanos = tNanos - startNanos;
			final double totalSampleIntervals = (double) totalNanos / sampleIntervalNanos;
			result = (double) totalCount / totalSampleIntervals;
		}
		return result;
	}

	private final synchronized double internalRate(final long tNanos) {
		final double result;
		final long totalCount = aTotalCount.get();
		if (NanosComparator.compare(tNanos, startNanos) < 0 || totalCount == 0) {
			result = 0;
		} else {
			final long rightNanos = samples.lastKey();
			if (NanosComparator.compare(tNanos, rightNanos) < 0) {
				result = internalRateAverage(tNanos);
			} else {
				final long effectiveRightNanos = Math.max(rightNanos, tNanos);
				result = samples.subMap(effectiveRightNanos - sampleIntervalNanos, false, effectiveRightNanos, true)
						.values()
						.stream()
						.mapToLong(AtomicLong::get)
						.reduce(0, Math::addExact);
			}
		}
		return result;
	}

	private final synchronized void gc() {
		final long totalCount = aTotalCount.get();
		if (totalCount % 1024 == 0) {
			final long rightNanos = samples.lastKey();
			@Nullable
			final Long rightNanosToRemoveTo = samples.floorKey(rightNanos - sampleIntervalNanos);
			if (rightNanosToRemoveTo != null) {
				samples.subMap(samples.firstKey(), true , rightNanosToRemoveTo, true)
						.clear();
			}
		}
	}

	@ThreadSafe
	private static final class NanosComparator implements Comparator<Long> {
		static final NanosComparator instance = new NanosComparator();

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
			return compare(o1, o2);
		}
	}
}