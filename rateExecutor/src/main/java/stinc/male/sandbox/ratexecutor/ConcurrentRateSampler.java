package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import static stinc.male.sandbox.ratexecutor.Preconditions.checkArgument;
import static stinc.male.sandbox.ratexecutor.Preconditions.checkNotNull;

@ThreadSafe
public final class ConcurrentRateSampler implements RateSampler {
	private final Nanos startNanos;
	private final Duration sampleInterval;
	private final long sampleIntervalNanos;
	private AtomicLong aTotalCount;
	private final ConcurrentSkipListMap<Long, AtomicLong> samples;

	public ConcurrentRateSampler(final long startNanos, final Duration sampleInterval) {
		checkArgument(startNanos != Double.NaN, "startNanos", "Must not be NaN");
		checkNotNull(sampleInterval, "sampleInterval");
		checkArgument(!sampleInterval.isZero(), "sampleInterval", "Must not be zero");
		checkArgument(!sampleInterval.isNegative(), "sampleInterval", "Must be positive");
		this.startNanos = new Nanos(startNanos);
		this.sampleInterval = sampleInterval;
		sampleIntervalNanos = sampleInterval.toNanos();
		aTotalCount = new AtomicLong();
		samples = new ConcurrentSkipListMap<>(NanosComparator.getInstance());
		samples.put(startNanos, new AtomicLong());
	}

	@Override
	public final long getStartNanos() {
		return startNanos.start;
	}

	@Override
	public final Duration getSampleInterval() {
		return sampleInterval;
	}

	@Override
	public final synchronized long rightSampleWindowBoundary() {
		return samples.lastKey();
	}

	@Override
	public final synchronized long numberOfCurrentTicks() {
		final long rightNanos = samples.lastKey();
		return internalCount(rightNanos - sampleIntervalNanos, rightNanos);
	}

	@Override
	public final synchronized void tick(final long count, final long tNanos) {
		startNanos.check(tNanos, "tNanos");
		if (count != 0) {
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
		final long rightNanos = samples.lastKey();
		checkArgument(tNanos >= rightNanos, "tNanos", "Must not be less than " + rightNanos);
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
				+ "(startNanos=" + startNanos.start
				+ ", sampleIntervalNanos" + sampleIntervalNanos
				+ ')';
	}

	private final synchronized double internalRateAverage(final long tNanos) {
		final long totalCount = aTotalCount.get();
		final long totalNanos = tNanos - startNanos.start;
		final double totalSampleIntervals = (double) totalNanos / sampleIntervalNanos;
		return (double) totalCount / totalSampleIntervals;
	}

	private final synchronized double internalRate(final long tNanos) {
		final double result;
		final long totalCount = aTotalCount.get();
		if (NanosComparator.compare(tNanos, startNanos.start) < 0 || totalCount == 0) {
			result = 0;
		} else {
			final long rightNanos = samples.lastKey();
			if (NanosComparator.compare(tNanos, rightNanos) < 0) {
				result = internalRateAverage(tNanos);
			} else {
				final long effectiveRightNanos = Math.max(rightNanos, tNanos);
				result = internalCount(effectiveRightNanos - sampleIntervalNanos, effectiveRightNanos);
			}
		}
		return result;
	}

	private final synchronized long internalCount(final long fromExclusiveNanos, final long toInclusiveNanos) {
		return samples.subMap(fromExclusiveNanos, false, toInclusiveNanos, true)
				.values()
				.stream()
				.mapToLong(AtomicLong::get)
				.reduce(0, Math::addExact);
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

	private static final class Nanos {
		private final long start;
		private final long l1;
		private final long r1;
		private final long l2;
		private final long r2;

		Nanos(final long start) {
			this.start = start;
			if (start <= 0) {
				l1 = start;
				r1 = start + Long.MAX_VALUE;
				l2 = 0;
				r2 = 0;
			} else {
				l1 = start;
				r1 = Long.MAX_VALUE;
				l2 = Long.MIN_VALUE;
				r2 = Long.MIN_VALUE + start - 1;
			}
		}

		final void check(final long nanos, final String paramName) {
			if (start <= 0) {
				checkArgument(NanosComparator.compare(l1, nanos) <= 0 && NanosComparator.compare(nanos, r1) <= 0,
						paramName, String.format("Must be in [%s; %s]", l1, r1));
			} else {
				checkArgument((NanosComparator.compare(l1, nanos) <= 0 && NanosComparator.compare(nanos, r1) <= 0)
						|| (NanosComparator.compare(l2, nanos) <= 0 && NanosComparator.compare(nanos, r2) <= 0),
						paramName, String.format("Must be in [%s; %s]\u222a[%s; %s]", l1, r1, l2, r2));
			}
			return;
		}
	}
}