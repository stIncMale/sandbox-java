package stinc.male.sandbox.ratmex.meter;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Function;
import javax.annotation.Nullable;
import stinc.male.sandbox.ratmex.util.internal.ConversionsAndChecks;
import stinc.male.sandbox.ratmex.util.internal.Preconditions;
import static stinc.male.sandbox.ratmex.util.internal.Preconditions.checkNotNull;

/**
 * @param <T>
 */
public abstract class AbstractRingBufferRateMeter<C extends ConcurrentRingBufferRateMeterConfig, T extends LongArray> extends AbstractRateMeter<C> {
  private final boolean sequential;
  private final T samplesHistory;//length is multiple of HL
  /*Same length as samples history;
    required to overcome problem which arises when the samples window was moved too far while we were accounting a new sample.*/
  @Nullable
  private final LockStrategy[] ticksCountLocks;
  private final long samplesWindowStepNanos;
  private long samplesWindowShiftSteps;
  @Nullable
  private final AtomicLong atomicSamplesWindowShiftSteps;
  @Nullable
  private final AtomicLong atomicCompletedSamplesWindowShiftSteps;
  @Nullable
  private final StampedLock ticksCountStampedLock;
  @Nullable
  private final WaitStrategy completedSamplesWindowShiftStepsWaitStrategy;
  private final long maxTicksCountAttempts;

  /**
   * @param startNanos Starting point that is used to calculate elapsed nanoseconds.
   * @param samplesInterval Size of the samples window.
   * @param config Additional configuration parameters.
   */
  AbstractRingBufferRateMeter(
      final long startNanos,
      final Duration samplesInterval,
      final C config,
      final Function<Integer, ? extends T> samplesHistorySupplier,
      final boolean sequential) {
    super(startNanos, samplesInterval, config);
    checkNotNull(samplesHistorySupplier, "samplesHistorySupplier");
    final long timeSensitivityNanos = config.getTimeSensitivity()
        .toNanos();
    final long samplesIntervalNanos = getSamplesIntervalNanos();
    Preconditions.checkArgument(timeSensitivityNanos <= samplesIntervalNanos, "config",
        () -> String.format("getTimeSensitivityNanos()=%s must be not greater than getSamplesIntervalNanos()=%s",
            timeSensitivityNanos, getSamplesIntervalNanos()));
    final int samplesIntervalArrayLength = (int)(samplesIntervalNanos / timeSensitivityNanos);
    Preconditions.checkArgument(
        samplesIntervalNanos / samplesIntervalArrayLength * samplesIntervalArrayLength == samplesIntervalNanos,
        "samplesInterval", () -> String.format(
            "The specified getSamplesInterval()=%sns and getTimeSensitivity()=%sns " +
                "can not be used together because samplesInterval can not be divided evenly by timeSensitivity",
            samplesIntervalNanos,
            timeSensitivityNanos));
    samplesWindowStepNanos = samplesIntervalNanos / samplesIntervalArrayLength;
    samplesHistory = samplesHistorySupplier.apply(config.getHl() * samplesIntervalArrayLength);
    if (sequential) {
      ticksCountStampedLock = null;
      ticksCountLocks = null;
      completedSamplesWindowShiftStepsWaitStrategy = null;
    } else {
      ticksCountStampedLock = new StampedLock();
      completedSamplesWindowShiftStepsWaitStrategy = config.getWaitStrategySupplier()
          .get();
      if (config.isStrictTick()) {
        ticksCountLocks = new LockStrategy[samplesHistory.length()];
        for (int idx = 0; idx < ticksCountLocks.length; idx++) {
          ticksCountLocks[idx] = config.getLockStrategySupplier()
              .get();
        }
      } else {
        ticksCountLocks = null;
      }
    }
    atomicSamplesWindowShiftSteps = sequential ? null : new AtomicLong();
    samplesWindowShiftSteps = 0;
    atomicCompletedSamplesWindowShiftSteps = sequential ? null : new AtomicLong();
    this.sequential = sequential;
    maxTicksCountAttempts = getConfig().getMaxTicksCountAttempts() < 3 ? 3 : getConfig().getMaxTicksCountAttempts();
  }

  @Override
  public long rightSamplesWindowBoundary() {
    final long result;
    if (sequential) {
      result = rightSamplesWindowBoundary(samplesWindowShiftSteps);
    } else {
      assert atomicSamplesWindowShiftSteps != null;
      result = rightSamplesWindowBoundary(atomicSamplesWindowShiftSteps.get());
    }
    return result;
  }

  @Override
  public long ticksCount() {
    long result = 0;
    if (sequential) {
      final long samplesWindowShiftSteps = this.samplesWindowShiftSteps;
      for (int idx = leftSamplesWindowIdx(samplesWindowShiftSteps), i = 0;
          i < samplesHistory.length() / getConfig().getHl();
          idx = nextSamplesWindowIdx(idx), i++) {
        result += samplesHistory.get(idx);
      }
    } else {
      assert atomicSamplesWindowShiftSteps != null;
      assert ticksCountStampedLock != null;
      long ticksCountReadLockStamp = 0;
      try {
        long samplesWindowShiftSteps = atomicSamplesWindowShiftSteps.get();
        for (long ri = 0; ri < maxTicksCountAttempts || ticksCountReadLockStamp != 0; ri++) {
          result = 0;
          waitForCompletedWindowShiftSteps(samplesWindowShiftSteps);
          final int leftSamplesWindowIdx = leftSamplesWindowIdx(samplesWindowShiftSteps);
          for (int idx = leftSamplesWindowIdx, i = 0;
              i < samplesHistory.length() / getConfig().getHl();
              idx = nextSamplesWindowIdx(idx), i++) {
            result += samplesHistory.get(idx);
          }
          final long newSamplesWindowShiftSteps = atomicSamplesWindowShiftSteps.get();
          if (newSamplesWindowShiftSteps - samplesWindowShiftSteps <= samplesHistory.length() - samplesHistory.length() / getConfig().getHl()) {
            //the samples window may has been moved while we were counting, but result is still correct
            break;
          } else {//the samples window has been moved too far
            samplesWindowShiftSteps = newSamplesWindowShiftSteps;
            if (ticksCountReadLockStamp == 0 &&
                ri >= maxTicksCountAttempts / 2 - 1) {//we have spent half of the read attempts, let us fall over to lock approach
              ticksCountReadLockStamp = ticksCountStampedLock.readLock();
            }
          }
        }
      } finally {
        if (ticksCountReadLockStamp != 0) {
          ticksCountStampedLock.unlockRead(ticksCountReadLockStamp);
        }
      }
    }
    return result;
  }

  @Override
  public RateMeterReading ticksCount(final RateMeterReading reading) {
    checkNotNull(reading, "reading");
    reading.setAccurate(true);
    long value = 0;
    long samplesWindowShiftSteps;
    if (sequential) {
      samplesWindowShiftSteps = this.samplesWindowShiftSteps;
      for (int idx = leftSamplesWindowIdx(samplesWindowShiftSteps), i = 0;
          i < samplesHistory.length() / getConfig().getHl();
          idx = nextSamplesWindowIdx(idx), i++) {
        value += samplesHistory.get(idx);
      }
    } else {
      assert atomicSamplesWindowShiftSteps != null;
      assert ticksCountStampedLock != null;
      samplesWindowShiftSteps = atomicSamplesWindowShiftSteps.get();
      long ticksCountReadLockStamp = 0;
      try {
        for (int ri = 0; ri < maxTicksCountAttempts || ticksCountReadLockStamp != 0; ri++) {
          value = 0;
          waitForCompletedWindowShiftSteps(samplesWindowShiftSteps);
          final int leftSamplesWindowIdx = leftSamplesWindowIdx(samplesWindowShiftSteps);
          for (int idx = leftSamplesWindowIdx, i = 0;
              i < samplesHistory.length() / getConfig().getHl();
              idx = nextSamplesWindowIdx(idx), i++) {
            value += samplesHistory.get(idx);
          }
          final long newSamplesWindowShiftSteps = atomicSamplesWindowShiftSteps.get();
          if (newSamplesWindowShiftSteps - samplesWindowShiftSteps <= samplesHistory.length() - samplesHistory.length() / getConfig().getHl()) {
            //the samples window may has been moved while we were counting, but result is still correct
            break;
          } else {//the samples window has been moved too far
            samplesWindowShiftSteps = newSamplesWindowShiftSteps;
            if (ticksCountReadLockStamp == 0 &&
                ri >= maxTicksCountAttempts / 2 - 1) {//we have spent half of the read attempts, let us fall over to lock approach
              ticksCountReadLockStamp = ticksCountStampedLock.readLock();
            }
          }
        }
      } finally {
        if (ticksCountReadLockStamp != 0) {
          ticksCountStampedLock.unlockRead(ticksCountReadLockStamp);
        }
      }
    }
    reading.setTNanos(rightSamplesWindowBoundary(samplesWindowShiftSteps))
        .setValue(value);
    return reading;
  }

  @Override
  public void tick(final long count, final long tNanos) {
    checkArgument(tNanos, "tNanos");
    if (count != 0) {
      final long targetSamplesWindowShiftSteps = samplesWindowShiftSteps(tNanos);
      long samplesWindowShiftSteps;
      if (sequential) {
        samplesWindowShiftSteps = this.samplesWindowShiftSteps;
      } else {
        assert atomicSamplesWindowShiftSteps != null;
        samplesWindowShiftSteps = atomicSamplesWindowShiftSteps.get();
      }
      if (samplesWindowShiftSteps - samplesHistory.length() < targetSamplesWindowShiftSteps) {//tNanos is within the samples history
        if (sequential) {
          final int targetIdx = rightSamplesWindowIdx(targetSamplesWindowShiftSteps);
          if (samplesWindowShiftSteps < targetSamplesWindowShiftSteps) {//we need to move the samples window
            this.samplesWindowShiftSteps = targetSamplesWindowShiftSteps;
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
          assert ticksCountStampedLock != null;
          assert atomicCompletedSamplesWindowShiftSteps != null;
          boolean moved = false;
          long ticksCountWriteLockStamp = 0;
          while (samplesWindowShiftSteps < targetSamplesWindowShiftSteps) {//move the samples window if we need to
            if (ticksCountWriteLockStamp == 0) {
              ticksCountWriteLockStamp = ticksCountStampedLock.isReadLocked() ? ticksCountStampedLock.writeLock() : 0;
            }
            moved = atomicSamplesWindowShiftSteps.compareAndSet(samplesWindowShiftSteps, targetSamplesWindowShiftSteps);
            if (moved) {
              break;
            } else {
              samplesWindowShiftSteps = atomicSamplesWindowShiftSteps.get();
            }
          }
          try {
            final int targetIdx = rightSamplesWindowIdx(targetSamplesWindowShiftSteps);
            if (moved) {
              assert samplesWindowShiftSteps < targetSamplesWindowShiftSteps;
              final long numberOfSteps = targetSamplesWindowShiftSteps - samplesWindowShiftSteps;
              waitForCompletedWindowShiftSteps(samplesWindowShiftSteps);
              if (numberOfSteps <= samplesHistory.length()) {//reset some samples
                for (int idx = nextSamplesWindowIdx(rightSamplesWindowIdx(samplesWindowShiftSteps)), i = 0;
                    i < numberOfSteps && i < samplesHistory.length();
                    idx = nextSamplesWindowIdx(idx), i++) {
                  tickResetSample(idx, idx == targetIdx ? count : 0);
                  final long expectedCompletedSamplesWindowShiftSteps = samplesWindowShiftSteps + i;
                  atomicCompletedSamplesWindowShiftSteps.compareAndSet(
                      expectedCompletedSamplesWindowShiftSteps,
                      expectedCompletedSamplesWindowShiftSteps + 1);//complete the current step
                }
              } else {//reset all samples
                for (int idx = 0; idx < samplesHistory.length(); idx++) {
                  tickResetSample(idx, idx == targetIdx ? count : 0);
                }
                long completedSamplesWindowShiftSteps = this.atomicCompletedSamplesWindowShiftSteps.get();
                //complete steps up to the targetSamplesWindowShiftSteps
                while (completedSamplesWindowShiftSteps < targetSamplesWindowShiftSteps &&
                    !(this.atomicCompletedSamplesWindowShiftSteps.compareAndSet(completedSamplesWindowShiftSteps, targetSamplesWindowShiftSteps))) {
                  completedSamplesWindowShiftSteps = this.atomicCompletedSamplesWindowShiftSteps.get();
                }
              }
            } else {
              waitForCompletedWindowShiftSteps(targetSamplesWindowShiftSteps);
              tickAccumulateSample(targetIdx, count, targetSamplesWindowShiftSteps);
            }
          } finally {
            if (ticksCountWriteLockStamp != 0) {
              ticksCountStampedLock.unlockWrite(ticksCountWriteLockStamp);
            }
          }
        }
      }
      getTicksTotalCounter().add(count);
    }
  }

  @Override
  public double rateAverage(final long tNanos) {
    checkArgument(tNanos, "tNanos");
    final long samplesWindowShiftSteps;
    if (sequential) {
      samplesWindowShiftSteps = this.samplesWindowShiftSteps;
    } else {
      assert atomicSamplesWindowShiftSteps != null;
      samplesWindowShiftSteps = atomicSamplesWindowShiftSteps.get();
    }
    final long rightNanos = rightSamplesWindowBoundary(samplesWindowShiftSteps);
    final long effectiveRightNanos;
    if (NanosComparator.compare(tNanos, rightNanos) <= 0) {//tNanos is within or behind the samples window
      effectiveRightNanos = rightNanos;
    } else {//tNanos is ahead of the samples window
      effectiveRightNanos = tNanos;
    }
    return ConversionsAndChecks.rateAverage(effectiveRightNanos, getSamplesIntervalNanos(), getStartNanos(), ticksTotalCount());
  }

  @Override
  public double rate(final long tNanos) {
    checkArgument(tNanos, "tNanos");
    final double result;
    final long samplesIntervalNanos = getSamplesIntervalNanos();
    final long samplesWindowShiftSteps;
    if (sequential) {
      samplesWindowShiftSteps = this.samplesWindowShiftSteps;
    } else {
      assert atomicSamplesWindowShiftSteps != null;
      samplesWindowShiftSteps = atomicSamplesWindowShiftSteps.get();
    }
    final long rightNanos = rightSamplesWindowBoundary(samplesWindowShiftSteps);
    final long leftNanos = rightNanos - samplesIntervalNanos;
    if (NanosComparator.compare(tNanos, leftNanos) <= 0) {//tNanos is behind the samples window, so return average over all samples
      result = ConversionsAndChecks.rateAverage(rightNanos, samplesIntervalNanos, getStartNanos(), ticksTotalCount());//this is the same as rateAverage()
    } else {//tNanos is within or ahead of the samples window
      final long effectiveLeftNanos = tNanos - samplesIntervalNanos;
      if (NanosComparator.compare(rightNanos, effectiveLeftNanos) <= 0) {
        //tNanos is way too ahead of the samples window and there are no samples for the requested tNanos
        result = 0;
      } else {
        final long effectiveRightNanos = NanosComparator.compare(tNanos, rightNanos) <= 0
            ? tNanos//tNanos is within the samples window
            : rightNanos;//tNanos is ahead of samples window
        final long count = count(effectiveLeftNanos, effectiveRightNanos);
        if (sequential) {
          result = count;
        } else {
          final long tNanosSamplesWindowShiftSteps = samplesWindowShiftSteps(tNanos);
          long newSamplesWindowShiftSteps = atomicSamplesWindowShiftSteps.get();
          if (newSamplesWindowShiftSteps - tNanosSamplesWindowShiftSteps <= samplesHistory.length()) {
            //the samples window may has been moved while we were counting, but count is still correct
            result = count;
          } else {//the samples window has been moved too far, return average
            getStats().ifPresent(ConcurrentRateMeterStats::accountFailedAccuracyEventForRate);
            result = ConversionsAndChecks.rateAverage(
                rightSamplesWindowBoundary(newSamplesWindowShiftSteps),
                samplesIntervalNanos,
                getStartNanos(),
                ticksTotalCount());//this is the same as rateAverage()
          }
        }
      }
    }
    return result;
  }

  @Override
  public RateMeterReading rate(final long tNanos, final RateMeterReading reading) {
    checkArgument(tNanos, "tNanos");
    checkNotNull(reading, "reading");
    reading.setTNanos(tNanos);
    reading.setAccurate(true);
    final boolean valueSet;
    final long samplesIntervalNanos = getSamplesIntervalNanos();
    final long samplesWindowShiftSteps;
    if (sequential) {
      samplesWindowShiftSteps = this.samplesWindowShiftSteps;
    } else {
      assert atomicSamplesWindowShiftSteps != null;
      samplesWindowShiftSteps = atomicSamplesWindowShiftSteps.get();
    }
    final long rightNanos = rightSamplesWindowBoundary(samplesWindowShiftSteps);
    final long leftNanos = rightNanos - samplesIntervalNanos;
    if (NanosComparator.compare(tNanos, leftNanos) <= 0) {//tNanos is behind the samples window, so return average over all samples
      reading.setTNanos(rightNanos);
      reading.setAccurate(false);
      final double value = ConversionsAndChecks.rateAverage(rightNanos, samplesIntervalNanos, getStartNanos(), ticksTotalCount());//this is the same as rateAverage()
      reading.setValue(Math.round(value), value, true);
      valueSet = true;
    } else {//tNanos is within or ahead of the samples window
      final long effectiveLeftNanos = tNanos - samplesIntervalNanos;
      if (NanosComparator.compare(rightNanos, effectiveLeftNanos) <= 0) {
        //tNanos is way too ahead of the samples window and there are no samples for the requested tNanos
        final long value = 0;
        reading.setValue(value);
        valueSet = true;
      } else {
        final long effectiveRightNanos = NanosComparator.compare(tNanos, rightNanos) <= 0
            ? tNanos//tNanos is within the samples window
            : rightNanos;//tNanos is ahead of samples window
        final long count = count(effectiveLeftNanos, effectiveRightNanos);
        if (sequential) {
          final long value = count;
          reading.setValue(value);
          valueSet = true;
        } else {
          final long tNanosSamplesWindowShiftSteps = samplesWindowShiftSteps(tNanos);
          long newSamplesWindowShiftSteps = atomicSamplesWindowShiftSteps.get();
          if (newSamplesWindowShiftSteps - tNanosSamplesWindowShiftSteps <= samplesHistory.length()) {
            //the samples window may has been moved while we were counting, but count is still correct
            final long value = count;
            reading.setValue(value);
            valueSet = true;
          } else {//the samples window has been moved too far, return average
            final long newRightNanos = rightSamplesWindowBoundary(newSamplesWindowShiftSteps);
            reading.setTNanos(newRightNanos);
            reading.setAccurate(false);
            final double value = ConversionsAndChecks.rateAverage(
                newRightNanos,
                samplesIntervalNanos,
                getStartNanos(),
                ticksTotalCount());//this is the same as rateAverage()
            reading.setValue(Math.round(value), value, true);
            valueSet = true;
            getStats().ifPresent(ConcurrentRateMeterStats::accountFailedAccuracyEventForRate);
          }
        }
      }
    }
    assert valueSet;
    return reading;
  }

  private final long lockTicksCount(final int idx) {
    return ticksCountLocks == null ? 0 : ticksCountLocks[idx].lock();
  }

  private final void unlockTicksCount(final int idx, final long stamp) {
    if (ticksCountLocks != null) {
      ticksCountLocks[idx].unlock(stamp);
    }
  }

  private final long sharedLockTicksCount(final int idx) {
    return ticksCountLocks == null ? 0 : ticksCountLocks[idx].sharedLock();
  }

  private final void unlockSharedTicksCount(final int idx, final long stamp) {
    if (ticksCountLocks != null) {
      ticksCountLocks[idx].unlockShared(stamp);
    }
  }

  private final void tickResetSample(final int idx, final long value) {
    final long stamp = lockTicksCount(idx);
    try {
      samplesHistory.set(idx, value);
    } finally {
      unlockTicksCount(idx, stamp);
    }
  }

  private final void tickAccumulateSample(final int targetIdx, final long delta, final long targetSamplesWindowShiftSteps) {
    if (sequential) {
      samplesHistory.add(targetIdx, delta);
    } else {
      assert atomicSamplesWindowShiftSteps != null;
      if (ticksCountLocks == null) {//not strict mode, no locking
        samplesHistory.add(targetIdx, delta);
        final long samplesWindowShiftSteps = atomicSamplesWindowShiftSteps.get();
        if (targetSamplesWindowShiftSteps < samplesWindowShiftSteps - samplesHistory.length()) {
          //we could have accounted (but it is not necessary) the sample at the incorrect instant because samples window had been moved too far
          getStats().ifPresent(ConcurrentRateMeterStats::accountFailedAccuracyEventForTick);
        }
      } else {
        final long stamp = sharedLockTicksCount(targetIdx);
        try {
          final long samplesWindowShiftSteps = atomicSamplesWindowShiftSteps.get();
          if (samplesWindowShiftSteps - samplesHistory.length() < targetSamplesWindowShiftSteps) {
            //double check that tNanos is still within the samples history
            samplesHistory.add(targetIdx, delta);
          }
        } finally {
          unlockSharedTicksCount(targetIdx, stamp);
        }
      }
    }
  }

  private final void waitForCompletedWindowShiftSteps(final long samplesWindowShiftSteps) {
    if (!sequential) {
      assert completedSamplesWindowShiftStepsWaitStrategy != null;
      assert atomicCompletedSamplesWindowShiftSteps != null;
      completedSamplesWindowShiftStepsWaitStrategy.await(() -> samplesWindowShiftSteps <= atomicCompletedSamplesWindowShiftSteps.get());
    }
  }

  private final long count(final long fromExclusiveNanos, final long toInclusiveNanos) {
    final long fromInclusiveNanos = fromExclusiveNanos + samplesWindowStepNanos;//fix for AbstractRateMeterUnitTest.rateBug
    long result = 0;
    if (fromInclusiveNanos <= toInclusiveNanos) {//fix for AbstractRateMeterUnitTest.rateHanging2
      final long fromInclusiveSamplesWindowShiftSteps = samplesWindowShiftSteps(fromInclusiveNanos);
      if (!sequential) {
        assert completedSamplesWindowShiftStepsWaitStrategy != null;
        assert atomicCompletedSamplesWindowShiftSteps != null;
        completedSamplesWindowShiftStepsWaitStrategy.await(() ->
            fromInclusiveSamplesWindowShiftSteps <= atomicCompletedSamplesWindowShiftSteps.get());
      }
      waitForCompletedWindowShiftSteps(fromInclusiveSamplesWindowShiftSteps);
      final long toInclusiveSamplesWindowShiftSteps = samplesWindowShiftSteps(toInclusiveNanos);
      for (int idx = rightSamplesWindowIdx(fromInclusiveSamplesWindowShiftSteps), i = 0;
          i < toInclusiveSamplesWindowShiftSteps - fromInclusiveSamplesWindowShiftSteps + 1;
          idx = nextSamplesWindowIdx(idx), i++) {
        result += samplesHistory.get(idx);
      }
    }
    return result;
  }

  private long rightSamplesWindowBoundary(final long samplesWindowShiftSteps) {
    return getStartNanos() + samplesWindowShiftSteps * samplesWindowStepNanos;
  }

  private final long samplesWindowShiftSteps(final long tNanos) {
    final long samplesWindowShiftNanos = tNanos - getStartNanos();
    final long result;
    if (samplesWindowShiftNanos % samplesWindowStepNanos <= 0) {//fix for AbstractRateMeterUnitTest.rateHanging1
      result = samplesWindowShiftNanos / samplesWindowStepNanos;
    } else {//samplesWindowShiftNanos > 0
      result = samplesWindowShiftNanos / samplesWindowStepNanos + 1;
    }
//    if (samplesWindowShiftNanos % samplesWindowStepNanos == 0) {
//      result = samplesWindowShiftNanos / samplesWindowStepNanos;
//    } else if (samplesWindowShiftNanos > 0) {
//      result = samplesWindowShiftNanos / samplesWindowStepNanos + 1;
//    } else {//fix for AbstractRateMeterUnitTest.rateHanging1
//      result = samplesWindowShiftNanos / samplesWindowStepNanos;
//    }
    return result;
  }

  private final int leftSamplesWindowIdx(final long samplesWindowShiftSteps) {
    //the result can not be greater than samples.length, which is int, so it is a safe cast to int
    return (int)((samplesWindowShiftSteps + samplesHistory.length() - samplesHistory.length() / getConfig().getHl()) % samplesHistory.length());
  }

  private final int rightSamplesWindowIdx(final long samplesWindowShiftSteps) {
    //the result can not be greater than samples.length, which is int, so it is a safe cast to int
    return (int)((samplesWindowShiftSteps + samplesHistory.length() - 1) % samplesHistory.length());
  }

  private final int nextSamplesWindowIdx(final int idx) {
    return (idx + 1) % samplesHistory.length();
  }
}