package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import static stinc.male.sandbox.ratexecutor.Preconditions.checkArgument;
import static stinc.male.sandbox.ratexecutor.Preconditions.checkNotNull;

@ThreadSafe
public final class ConcurrentRateSampler implements RateSampler {
	private final long startNanos;
	private final long sampleIntervalNanos;
	private final List<Sample> samples;//(rightT - sampleIntervalNanos; rightT]
	private int leftIdx;//samples(leftIdx) > (rightT - sampleIntervalNanos)

	public ConcurrentRateSampler(final long startNanos, final Duration sampleInterval) {
		checkArgument(startNanos != Double.NaN, "startNanos", "Must not be NaN");
		checkNotNull(sampleInterval, "sampleInterval");
		checkArgument(!sampleInterval.isZero(), "sampleInterval", "Must not be zero");
		checkArgument(!sampleInterval.isNegative(), "sampleInterval", "Must be positive");
		this.startNanos = startNanos;
		sampleIntervalNanos = sampleInterval.toNanos();
		samples = new ArrayList<>();
	}

	@Override
	public final synchronized double tick(final long count, final long instantNanos) {
		checkArgument(count >= 0, "count", "Must not be negative");
		checkArgument(instantNanos >= startNanos, "instantNanos", "Must not be less than startNanos");
		return 0;
	}

	@Override
	public final synchronized double rate() {
		final long samplesTotalCount = samples.stream()
				.skip(leftIdx)
				.mapToLong(sample -> sample.count)
				.reduce(0, Math::addExact);
		return samplesTotalCount;
	}

	@Override
	public final synchronized String toString() {
		return new StringBuilder(getClass().getSimpleName())
				.append("(startNanos=").append(startNanos)
				.append(", sampleIntervalNanos").append(sampleIntervalNanos)
				.append(')').toString();
	}

	@Immutable
	private static final class Sample {
		final long count;
		final long t;

		Sample(final long count, final long t) {
			this.count = count;
			this.t = t;
		}

		@Override
		public final String toString() {
			return new StringBuilder(getClass().getSimpleName())
					.append("(count=").append(count)
					.append(", t").append(t)
					.append(')').toString();
		}
	}
}