package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.LongAdder;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * This implementation of {@link RateMeter} is not linearizable and may produce inaccurate results if used concurrently
 * (see {@link RateMeter} for details).
 */
@ThreadSafe
public class AtomicArrayRateMeter extends AbstractRateMeter {
  private static final RateMeterConfig defaultInstance = RateMeterConfig.newBuilder()
      .setTicksCounterSupplier(LongAdderTicksCounter::new)
      .build();
  private static final int MAX_OPTIMISTIC_READ_ATTEMPTS = 3;//TODO config

  private final AtomicLongArray samples;//length is even
  @Nullable
  private final AtomicBoolean[] tickLocks;//same length as samples; required to overcome problem which arises when the samples window moved too far while we were executing tick method
  private final long samplesWindowStepNanos;
  private AtomicLong samplesWindowShiftSteps;
  private AtomicLong completedSamplesWindowShiftSteps;
  private final LongAdder failedAccuracyEventsCount;

  /**
   * @return A reasonable configuration.
   */
  public static final RateMeterConfig defaultConfig() {
    return defaultInstance;
  }

  /**
   * @param startNanos Starting point that is used to calculate elapsed nanoseconds.
   * @param samplesInterval Size of the samples window.
   * @param config Additional configuration parameters.
   */
  public AtomicArrayRateMeter(
      final long startNanos,
      final Duration samplesInterval,
      final RateMeterConfig config) {
    super(startNanos, samplesInterval, config);
    final long sensitivityNanos = config.getTimeSensitivity().toNanos();
    final long samplesIntervalNanos = getSamplesIntervalNanos();
    final int samplesIntervalArrayLength = (int) (samplesIntervalNanos / sensitivityNanos);
    Preconditions.checkArgument(samplesIntervalNanos / samplesIntervalArrayLength * samplesIntervalArrayLength == samplesIntervalNanos, "samplesInterval",
        () -> String.format(
            "The specified samplesInterval %snanos and timeSensitivity %snanos can not be used together because samplesInterval can not be devided evenly by timeSensitivity",
            samplesIntervalArrayLength, sensitivityNanos));
    samplesWindowStepNanos = samplesIntervalNanos / samplesIntervalArrayLength;
    samples = new AtomicLongArray(2 * samplesIntervalArrayLength);
    if (true) {//TODO config
      tickLocks = new AtomicBoolean[samples.length()];
      for (int idx = 0; idx < tickLocks.length; idx++) {
        tickLocks[idx] = new AtomicBoolean();
      }
    } else {
      tickLocks = null;
    }
    samplesWindowShiftSteps = new AtomicLong();
    completedSamplesWindowShiftSteps = new AtomicLong();
    failedAccuracyEventsCount = new LongAdder();
  }

  /**
   * Acts like {@link #AtomicArrayRateMeter(long, Duration, RateMeterConfig)} with {@link #defaultConfig()}
   * as the third argument.
   */
  public AtomicArrayRateMeter(final long startNanos, final Duration samplesInterval) {
    this(startNanos, samplesInterval, defaultConfig());
  }

  @Override
  public long rightSamplesWindowBoundary() {
    return rightSamplesWindowBoundary(samplesWindowShiftSteps.get());
  }

  @Override
  public long ticksCount() {
    long samplesWindowShiftSteps = this.samplesWindowShiftSteps.get();
    long result = 0;
    for (int ri = 0; ri < MAX_OPTIMISTIC_READ_ATTEMPTS; ri++) {
      result = 0;
      waitForCompletedWindowShiftSteps(samplesWindowShiftSteps);
      for (int idx = leftSamplesWindowIdx(samplesWindowShiftSteps), i = 0;
          i < samples.length() / 2;
          idx = nextSamplesWindowIdx(idx), i++) {
        result += samples.get(idx);
      }
      final long newSamplesWindowShiftSteps = this.samplesWindowShiftSteps.get();
      if (newSamplesWindowShiftSteps - samplesWindowShiftSteps <= samples.length() / 2) {//the samples window may has been moved while we were counting, but result is still correct
        break;
      } else {//the samples window has been moved too far
        samplesWindowShiftSteps = newSamplesWindowShiftSteps;
        if (ri == MAX_OPTIMISTIC_READ_ATTEMPTS - 1) {//all read attempts have been exhausted, return what we have
          failedAccuracyEventsCount.increment();
        }
      }
    }
    return result;
  }

  @Override
  public void tick(final long count, final long tNanos) {
    checkArgument(tNanos, "tNanos");
    if (count != 0) {
      final long targetSamplesWindowShiftSteps = samplesWindowShiftSteps(tNanos);
      long samplesWindowShiftSteps = this.samplesWindowShiftSteps.get();
      if (samplesWindowShiftSteps - samples.length() < targetSamplesWindowShiftSteps) {//tNanos is within the samples history
        boolean moved = false;
        while (samplesWindowShiftSteps < targetSamplesWindowShiftSteps
            && !(moved = this.samplesWindowShiftSteps.compareAndSet(samplesWindowShiftSteps, targetSamplesWindowShiftSteps))) {//move the samples window if we we need to
          samplesWindowShiftSteps = this.samplesWindowShiftSteps.get();
        }
        final int targetIdx = rightSamplesWindowIdx(targetSamplesWindowShiftSteps);
        if (moved) {
          final long numberOfSteps = targetSamplesWindowShiftSteps - samplesWindowShiftSteps;
          waitForCompletedWindowShiftSteps(samplesWindowShiftSteps);
          if (numberOfSteps <= samples.length()) {//reset some samples
            for (int idx = nextSamplesWindowIdx(rightSamplesWindowIdx(samplesWindowShiftSteps)), i = 0;
                i < numberOfSteps;
                idx = nextSamplesWindowIdx(idx), i++) {
              tickResetSample(idx, idx == targetIdx ? count : 0, tickLock(idx));
              final long expectedCompletedSamplesWindowShiftSteps = samplesWindowShiftSteps + i;
              this.completedSamplesWindowShiftSteps.compareAndSet(expectedCompletedSamplesWindowShiftSteps, expectedCompletedSamplesWindowShiftSteps + 1);//complete the reset step
            }
          } else {//reset all samples
            for (int idx = 0; idx < samples.length(); idx++) {
              tickResetSample(idx, idx == targetIdx ? count : 0, tickLock(idx));
            }
            long completedSamplesWindowShiftSteps = this.completedSamplesWindowShiftSteps.get();
            while (completedSamplesWindowShiftSteps < targetSamplesWindowShiftSteps
                && !(this.completedSamplesWindowShiftSteps.compareAndSet(completedSamplesWindowShiftSteps, targetSamplesWindowShiftSteps))) {//complete steps
              completedSamplesWindowShiftSteps = this.completedSamplesWindowShiftSteps.get();
            }
          }
        } else {
          waitForCompletedWindowShiftSteps(targetSamplesWindowShiftSteps);
          tickAccumulateSample(targetIdx, count, tickLock(targetIdx), targetSamplesWindowShiftSteps);
        }
      }
      getTicksTotalCounter().add(count);
    }
  }

  @Override
  public double rateAverage(final long tNanos) {
    checkArgument(tNanos, "tNanos");
    final long samplesIntervalNanos = getSamplesIntervalNanos();
    final long samplesWindowShiftSteps = this.samplesWindowShiftSteps.get();
    final long rightNanos = rightSamplesWindowBoundary(samplesWindowShiftSteps);
    final long leftNanos = rightNanos - samplesIntervalNanos;
    final long count;
    final long effectiveTNanos;
    if (NanosComparator.compare(tNanos, leftNanos) <= 0) {//tNanos is behind the samples window, so return average over all samples
      count = ticksTotalCount();
      effectiveTNanos = rightNanos;
    } else if (NanosComparator.compare(rightNanos, tNanos) <= 0) {//tNanos is ahead of the samples window or exactly on the right border
      count = ticksTotalCount();
      effectiveTNanos = tNanos;
    } else {//tNanos is within the samples window and not on the right border
      final long substractCount = count(tNanos, rightNanos);
      final long tNanosSamplesWindowShiftSteps = samplesWindowShiftSteps(tNanos);
      final long newSamplesWindowShiftSteps = this.samplesWindowShiftSteps.get();
      if (newSamplesWindowShiftSteps - tNanosSamplesWindowShiftSteps <= samples.length()) {//the samples window may has been moved while we were counting, but substractCount is still correct
        count = ticksTotalCount() - substractCount;
        effectiveTNanos = rightSamplesWindowBoundary(tNanosSamplesWindowShiftSteps);
      } else {//the samples window has been moved too far, so average over all samples is the best we can do
        failedAccuracyEventsCount.increment();
        count = ticksTotalCount();
        effectiveTNanos = rightSamplesWindowBoundary(newSamplesWindowShiftSteps);
      }
    }
    return RateMeterMath.rateAverage(effectiveTNanos, samplesIntervalNanos, getStartNanos(), count);
  }

  @Override
  public double rate(final long tNanos) {
    checkArgument(tNanos, "tNanos");
    final double result;
    final long samplesIntervalNanos = getSamplesIntervalNanos();
    final long samplesWindowShiftSteps = this.samplesWindowShiftSteps.get();
    final long rightNanos = rightSamplesWindowBoundary(samplesWindowShiftSteps);
    final long leftNanos = rightNanos - samplesIntervalNanos;
    if (NanosComparator.compare(tNanos, leftNanos) <= 0) {//tNanos is behind the samples window, so return average over all samples
      result = RateMeterMath.rateAverage(rightNanos, samplesIntervalNanos, getStartNanos(), ticksTotalCount());//this is the same as rateAverage(), or rateAverage(rightNanos) if rightNanos == rightSamplesWindowBoundary()
    } else {//tNanos is within or ahead of the samples window
      final long effectiveLeftNanos = tNanos - samplesIntervalNanos;
      if (NanosComparator.compare(rightNanos, effectiveLeftNanos) <= 0) {//tNanos is way too ahead of the samples window and the are no samples for the requested tNanos
        result = 0;
      } else {
        long count = count(effectiveLeftNanos, rightNanos);
        final long tNanosSamplesWindowShiftSteps = samplesWindowShiftSteps(tNanos);
        long newSamplesWindowShiftSteps = this.samplesWindowShiftSteps.get();
        if (newSamplesWindowShiftSteps - tNanosSamplesWindowShiftSteps <= samples.length()) {//the samples window may has been moved while we were counting, but count is still correct
          result = count;
        } else {//the samples window has been moved too far
          failedAccuracyEventsCount.increment();
          if (NanosComparator.compare(tNanos, rightNanos) < 0 && newSamplesWindowShiftSteps - samplesWindowShiftSteps <= samples.length()) {//tNanos is within the samples window and we still have a chance to calculate rate for rightNanos
            count = count(leftNanos, rightNanos);
            newSamplesWindowShiftSteps = this.samplesWindowShiftSteps.get();
            if (newSamplesWindowShiftSteps - samplesWindowShiftSteps <= samples.length()) {//the samples window may has been moved while we were counting, but count is still correct
              result = count;
            } else {//average over all samples is the best we can do
              result = RateMeterMath.rateAverage(rightSamplesWindowBoundary(newSamplesWindowShiftSteps), samplesIntervalNanos, getStartNanos(), ticksTotalCount());//this is the same as rateAverage(), or rateAverage(rightNanos) if rightNanos == rightSamplesWindowBoundary()
            }
          } else {//average over all samples is the best we can do
            result = RateMeterMath.rateAverage(rightSamplesWindowBoundary(newSamplesWindowShiftSteps), samplesIntervalNanos, getStartNanos(), ticksTotalCount());//this is the same as rateAverage(), or rateAverage(rightNanos) if rightNanos == rightSamplesWindowBoundary()
          }
        }
      }
    }
    return result;
  }

  public long rightSamplesWindowBoundary(final long samplesWindowShiftSteps) {
    return getStartNanos() + samplesWindowShiftSteps * samplesWindowStepNanos;
  }

  public final long failedAccuracyEventsCount() {//TODO interface
    return failedAccuracyEventsCount.sum();
  }

  @Nullable
  private final AtomicBoolean tickLock(final int idx) {
    return tickLocks == null ? null : tickLocks[idx];
  }

  private final void tickResetSample(final int idx, final long value, @Nullable final AtomicBoolean lock) {
    while (lock != null && !lock.compareAndSet(false, true)) {
      Thread.yield();
    }
    try {
      samples.set(idx, value);
    } finally {
      if (lock != null) {
        lock.set(false);
      }
    }
  }

  private final void tickAccumulateSample(final int targetIdx, final long delta, @Nullable final AtomicBoolean lock, final long targetSamplesWindowShiftSteps) {
    while (lock != null && !lock.compareAndSet(false, true)) {
      Thread.yield();
    }
    try {
      if (lock == null) {
        samples.getAndAdd(targetIdx, delta);
        if (targetSamplesWindowShiftSteps < this.samplesWindowShiftSteps.get() - samples.length()) {//we could have accounted the sample at the incorrect instant because samples window was moved too far
          this.failedAccuracyEventsCount.increment();
        }
      } else {
        if (this.samplesWindowShiftSteps.get() - samples.length() < targetSamplesWindowShiftSteps) {//tNanos is still within the samples history
          samples.set(targetIdx, samples.get(targetIdx) + delta);//we are under lock, so no need in CAS
        }
      }
    } finally {
      if (lock != null) {
        lock.set(false);
      }
    }
  }

  private final void waitForCompletedWindowShiftSteps(final long samplesWindowShiftSteps) {
    while (this.completedSamplesWindowShiftSteps.get() < samplesWindowShiftSteps) {
      Thread.yield();
    }
  }

  private final long count(final long fromExclusiveNanos, final long toInclusiveNanos) {
    final long fromInclusiveNanos = fromExclusiveNanos + 1;
    final long fromInclusiveSamplesWindowShiftSteps = samplesWindowShiftSteps(fromInclusiveNanos);
    waitForCompletedWindowShiftSteps(fromInclusiveSamplesWindowShiftSteps);
    final long toInclusiveSamplesWindowShiftSteps = samplesWindowShiftSteps(toInclusiveNanos);
    long result = 0;
    for (int idx = rightSamplesWindowIdx(fromInclusiveSamplesWindowShiftSteps), i = 0;
        i < toInclusiveSamplesWindowShiftSteps - fromInclusiveSamplesWindowShiftSteps + 1;
        idx = nextSamplesWindowIdx(idx), i++) {
      result += samples.get(idx);
    }
    return result;
  }

  private final long samplesWindowShiftSteps(final long tNanos) {
    final long samplesWindowShiftNanos = tNanos - getStartNanos();
    return samplesWindowShiftNanos % samplesWindowStepNanos == 0
        ? samplesWindowShiftNanos / samplesWindowStepNanos
        : samplesWindowShiftNanos / samplesWindowStepNanos + 1;
  }

  private final int leftSamplesWindowIdx(final long samplesWindowShiftSteps) {
    return (int) (Math.abs((samplesWindowShiftSteps + samples.length() / 2) % samples.length()));//the result can not be greater than samples.length, which is int, so it is a safe cast to int
  }

  private final int rightSamplesWindowIdx(final long samplesWindowShiftSteps) {
    return (int) (Math.abs((samplesWindowShiftSteps + samples.length() - 1) % samples.length()));//the result can not be greater than samples.length, which is int, so it is a safe cast to int
  }

  private final int nextSamplesWindowIdx(final int idx) {
    return (idx + 1) % samples.length();
  }
}