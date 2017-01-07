package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import javax.annotation.Nullable;
import static stinc.male.sandbox.ratexecutor.Preconditions.checkNotNull;

/**
 * This implementation of {@link RateMeter} is not linearizable and may produce inaccurate results if used concurrently
 * (see {@link RateMeter} for details).
 * @param <T>
 */
public abstract class AbstractRingBufferRateMeter<T extends LongArray> extends AbstractRateMeter {
  private final boolean sequential;
  private final T samplesHistory;//length is multiple of HL
  @Nullable
  private final AtomicBoolean[] locks;//same length as samples history; required to overcome problem which arises when the samples window was moved too far while we were accounting a new sample
  private final long samplesWindowStepNanos;
  private long samplesWindowShiftSteps_todo;
  @Nullable
  private final AtomicLong samplesWindowShiftSteps;
  @Nullable
  private final AtomicLong completedSamplesWindowShiftSteps;

  /**
   * @param startNanos Starting point that is used to calculate elapsed nanoseconds.
   * @param samplesInterval Size of the samples window.
   * @param config Additional configuration parameters.
   * @param samplesHistorySuppplier
   * @param sequential
   */
  AbstractRingBufferRateMeter(
      final long startNanos,
      final Duration samplesInterval,
      final ConcurrentRingBufferRateMeterConfig config,
      final Function<Integer, ? extends T> samplesHistorySuppplier,
      final boolean sequential) {
    super(startNanos, samplesInterval, config);
    checkNotNull(samplesHistorySuppplier, "samplesHistorySuppplier");
    final long timeSensitivityNanos = config.getTimeSensitivity().toNanos();
    final long samplesIntervalNanos = getSamplesIntervalNanos();
    Preconditions.checkArgument(timeSensitivityNanos <= samplesIntervalNanos, "config",
        () -> String.format("getTimeSensitivityNanos()=%s must be not greater than getSamplesIntervalNanos()=%s",
            timeSensitivityNanos, getSamplesIntervalNanos()));
    final int samplesIntervalArrayLength = (int) (samplesIntervalNanos / timeSensitivityNanos);
    Preconditions.checkArgument(samplesIntervalNanos / samplesIntervalArrayLength * samplesIntervalArrayLength == samplesIntervalNanos, "samplesInterval",
        () -> String.format(
            "The specified getSamplesInterval()=%snanos and getTimeSensitivity()=%snanos can not be used together because samplesInterval can not be devided evenly by timeSensitivity",
            samplesIntervalArrayLength, timeSensitivityNanos));
    samplesWindowStepNanos = samplesIntervalNanos / samplesIntervalArrayLength;
    samplesHistory = samplesHistorySuppplier.apply(config.getHl() * samplesIntervalArrayLength);
    if (!sequential && config.isStrictTick()) {
      locks = new AtomicBoolean[samplesHistory.length()];
      for (int idx = 0; idx < locks.length; idx++) {
        locks[idx] = new AtomicBoolean();
      }
    } else {
      locks = null;
    }
    samplesWindowShiftSteps = sequential ? null : new AtomicLong();
    samplesWindowShiftSteps_todo = 0;
    completedSamplesWindowShiftSteps = sequential ? null : new AtomicLong();
    this.sequential = sequential;
  }

  @Override
  public long rightSamplesWindowBoundary() {
    return sequential
        ? rightSamplesWindowBoundary(samplesWindowShiftSteps_todo)
        : rightSamplesWindowBoundary(samplesWindowShiftSteps.get());
  }

  @Override
  public long ticksCount() {
    long result = 0;
    if (sequential) {
      final long samplesWindowShiftSteps = this.samplesWindowShiftSteps_todo;
      for (int idx = leftSamplesWindowIdx(samplesWindowShiftSteps), i = 0;
          i < samplesHistory.length() / getConfig().getHl();
          idx = nextSamplesWindowIdx(idx), i++) {
        result += samplesHistory.get(idx);
      }
    } else {
      long samplesWindowShiftSteps = this.samplesWindowShiftSteps.get();
      for (long ri = 0; ri < getConfig().getMaxTicksCountAttempts(); ri++) {
        result = 0;
        waitForCompletedWindowShiftSteps(samplesWindowShiftSteps);
        final int leftSamplesWindowIdx = leftSamplesWindowIdx(samplesWindowShiftSteps);
        for (int idx = leftSamplesWindowIdx, i = 0;
            i < samplesHistory.length() / getConfig().getHl();
            idx = nextSamplesWindowIdx(idx), i++) {
          result += samplesHistory.get(idx);
        }
        final long newSamplesWindowShiftSteps = this.samplesWindowShiftSteps.get();
        if (newSamplesWindowShiftSteps - samplesWindowShiftSteps <= samplesHistory.length() - samplesHistory.length() / getConfig().getHl()) {//the samples window may has been moved while we were counting, but result is still correct
          break;
        } else {//the samples window has been moved too far
          samplesWindowShiftSteps = newSamplesWindowShiftSteps;
          if (ri == getConfig().getMaxTicksCountAttempts() - 1) {//all read attempts have been exhausted, return what we have
            getStats().accountFailedAccuracyEventForTicksCount();
          }
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
      long samplesWindowShiftSteps = sequential ? this.samplesWindowShiftSteps_todo : this.samplesWindowShiftSteps.get();
      if (samplesWindowShiftSteps - samplesHistory.length() < targetSamplesWindowShiftSteps) {//tNanos is within the samples history
        if (sequential) {
          final int targetIdx = rightSamplesWindowIdx(targetSamplesWindowShiftSteps);
          if (samplesWindowShiftSteps < targetSamplesWindowShiftSteps) {//we need to move the samples window
            this.samplesWindowShiftSteps_todo = targetSamplesWindowShiftSteps;
            final long numberOfSteps = targetSamplesWindowShiftSteps - samplesWindowShiftSteps;
            for (int idx = nextSamplesWindowIdx(rightSamplesWindowIdx(samplesWindowShiftSteps)), i = 0;
                i < numberOfSteps && i < samplesHistory.length();
                idx = nextSamplesWindowIdx(idx), i++) {//reset moved samples
              samplesHistory.set(idx, idx == targetIdx ? count : 0);
            }
          } else {
            samplesHistory.add(targetIdx, count);
          }
        } else {
          boolean moved = false;
          while (samplesWindowShiftSteps < targetSamplesWindowShiftSteps
              && !(moved = this.samplesWindowShiftSteps.compareAndSet(samplesWindowShiftSteps, targetSamplesWindowShiftSteps))) {//move the samples window if we we need to
            samplesWindowShiftSteps = this.samplesWindowShiftSteps.get();
          }
          final int targetIdx = rightSamplesWindowIdx(targetSamplesWindowShiftSteps);
          if (moved) {
            final long numberOfSteps = targetSamplesWindowShiftSteps - samplesWindowShiftSteps;
            waitForCompletedWindowShiftSteps(samplesWindowShiftSteps);
            if (numberOfSteps <= samplesHistory.length()) {//reset some samples
              for (int idx = nextSamplesWindowIdx(rightSamplesWindowIdx(samplesWindowShiftSteps)), i = 0;
                  i < numberOfSteps && i < samplesHistory.length();
                  idx = nextSamplesWindowIdx(idx), i++) {
                tickResetSample(idx, idx == targetIdx ? count : 0);
                final long expectedCompletedSamplesWindowShiftSteps = samplesWindowShiftSteps + i;
                this.completedSamplesWindowShiftSteps.compareAndSet(expectedCompletedSamplesWindowShiftSteps, expectedCompletedSamplesWindowShiftSteps + 1);//complete the reset step
              }
            } else {//reset all samples
              for (int idx = 0; idx < samplesHistory.length(); idx++) {
                tickResetSample(idx, idx == targetIdx ? count : 0);
              }
              long completedSamplesWindowShiftSteps = this.completedSamplesWindowShiftSteps.get();
              while (completedSamplesWindowShiftSteps < targetSamplesWindowShiftSteps
                  && !(this.completedSamplesWindowShiftSteps.compareAndSet(completedSamplesWindowShiftSteps, targetSamplesWindowShiftSteps))) {//complete steps
                completedSamplesWindowShiftSteps = this.completedSamplesWindowShiftSteps.get();
              }
            }
          } else {
            waitForCompletedWindowShiftSteps(targetSamplesWindowShiftSteps);
            tickAccumulateSample(targetIdx, count, targetSamplesWindowShiftSteps);
          }
        }
      }
      getTicksTotalCounter().add(count);
    }
  }

  @Override
  public double rateAverage(final long tNanos) {
    checkArgument(tNanos, "tNanos");
    final long samplesIntervalNanos = getSamplesIntervalNanos();
    final long samplesWindowShiftSteps = sequential ? this.samplesWindowShiftSteps_todo : this.samplesWindowShiftSteps.get();
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
      if (sequential) {
        count = ticksTotalCount() - substractCount;
        effectiveTNanos = tNanos;
      } else {
        final long tNanosSamplesWindowShiftSteps = samplesWindowShiftSteps(tNanos);
        final long newSamplesWindowShiftSteps = this.samplesWindowShiftSteps.get();
        if (newSamplesWindowShiftSteps - tNanosSamplesWindowShiftSteps <= samplesHistory.length()) {//the samples window may has been moved while we were counting, but substractCount is still correct
          count = ticksTotalCount() - substractCount;
          effectiveTNanos = rightSamplesWindowBoundary(tNanosSamplesWindowShiftSteps);
        } else {//the samples window has been moved too far, so average over all samples is the best we can do
          getStats().accountFailedAccuracyEventForRateAverage();
          count = ticksTotalCount();
          effectiveTNanos = rightSamplesWindowBoundary(newSamplesWindowShiftSteps);
        }
      }
    }
    return RateMeterMath.rateAverage(effectiveTNanos, samplesIntervalNanos, getStartNanos(), count);
  }

  @Override
  public double rate(final long tNanos) {
    checkArgument(tNanos, "tNanos");
    final double result;
    final long samplesIntervalNanos = getSamplesIntervalNanos();
    final long samplesWindowShiftSteps = sequential ? this.samplesWindowShiftSteps_todo : this.samplesWindowShiftSteps.get();
    final long rightNanos = rightSamplesWindowBoundary(samplesWindowShiftSteps);
    final long leftNanos = rightNanos - samplesIntervalNanos;
    if (NanosComparator.compare(tNanos, leftNanos) <= 0) {//tNanos is behind the samples window, so return average over all samples
      result = RateMeterMath.rateAverage(rightNanos, samplesIntervalNanos, getStartNanos(), ticksTotalCount());//this is the same as rateAverage(), or rateAverage(rightNanos) if rightNanos == rightSamplesWindowBoundary()
    } else {//tNanos is within or ahead of the samples window
      final long effectiveLeftNanos = tNanos - samplesIntervalNanos;
      if (NanosComparator.compare(rightNanos, effectiveLeftNanos) <= 0) {//tNanos is way too ahead of the samples window and there are no samples for the requested tNanos
        result = 0;
      } else {
        final long count = NanosComparator.compare(tNanos, rightNanos) <= 0
            ? count(effectiveLeftNanos, tNanos)//tNanos is within the samples window
            : count(effectiveLeftNanos, rightNanos);//tNanos is ahead of samples window
        if (sequential) {
          result = count;
        } else {
          final long tNanosSamplesWindowShiftSteps = samplesWindowShiftSteps(tNanos);
          long newSamplesWindowShiftSteps = this.samplesWindowShiftSteps.get();
          if (newSamplesWindowShiftSteps - tNanosSamplesWindowShiftSteps <= samplesHistory.length()) {//the samples window may has been moved while we were counting, but count is still correct
            result = count;
          } else {//the samples window has been moved too far, return average
            getStats().accountFailedAccuracyEventForRate();
            result = RateMeterMath.rateAverage(
                rightSamplesWindowBoundary(newSamplesWindowShiftSteps), samplesIntervalNanos, getStartNanos(), ticksTotalCount());//this is the same as rateAverage(), or rateAverage(rightNanos) if rightNanos == rightSamplesWindowBoundary()
          }
        }
      }
    }
    return result;
  }

  private long rightSamplesWindowBoundary(final long samplesWindowShiftSteps) {
    return getStartNanos() + samplesWindowShiftSteps * samplesWindowStepNanos;
  }

  private final void lock(final int idx) {
    while (locks != null && !locks[idx].compareAndSet(false, true)) {
      Thread.yield();
    }
  }

  private final void unlock(final int idx) {
    if (locks != null) {
      locks[idx].set(false);
    }
  }

  private final void tickResetSample(final int idx, final long value) {
    lock(idx);
    try {
      samplesHistory.set(idx, value);
    } finally {
      unlock(idx);
    }
  }

  private final void tickAccumulateSample(final int targetIdx, final long delta, final long targetSamplesWindowShiftSteps) {
    if (sequential) {
      samplesHistory.add(targetIdx, delta);
    } else {
      lock(targetIdx);
      try {
        if (locks == null) {//there is no actual locking
          samplesHistory.add(targetIdx, delta);
          final long samplesWindowShiftSteps = this.samplesWindowShiftSteps.get();
          if (targetSamplesWindowShiftSteps < samplesWindowShiftSteps - samplesHistory.length()) {//we could have accounted (but it is not necessary) the sample at the incorrect instant because samples window had been moved too far
            getStats().accountFailedAccuracyEventForTick();
          }
        } else {
          final long samplesWindowShiftSteps = this.samplesWindowShiftSteps.get();
          if (samplesWindowShiftSteps - samplesHistory.length() < targetSamplesWindowShiftSteps) {//tNanos is still within the samples history
            samplesHistory.set(targetIdx, samplesHistory.get(targetIdx) + delta);//we are under lock, so no need in CAS
          }
        }
      } finally {
        unlock(targetIdx);
      }
    }
  }

  private final void waitForCompletedWindowShiftSteps(final long samplesWindowShiftSteps) {
    if (!sequential) {
      while (this.completedSamplesWindowShiftSteps.get() < samplesWindowShiftSteps) {
        Thread.yield();
      }
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
      result += samplesHistory.get(idx);
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
    return (int)((samplesWindowShiftSteps + samplesHistory.length() - samplesHistory.length() / getConfig().getHl()) % samplesHistory.length());//the result can not be greater than samples.length, which is int, so it is a safe cast to int
  }

  private final int rightSamplesWindowIdx(final long samplesWindowShiftSteps) {
    return (int)((samplesWindowShiftSteps + samplesHistory.length() - 1) % samplesHistory.length());//the result can not be greater than samples.length, which is int, so it is a safe cast to int
  }

  private final int nextSamplesWindowIdx(final int idx) {
    return (idx + 1) % samplesHistory.length();
  }
}