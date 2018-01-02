package stinc.male.sandbox.ratmex.meter;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Function;
import javax.annotation.Nullable;
import stinc.male.sandbox.ratmex.internal.util.ConversionsAndChecks;
import stinc.male.sandbox.ratmex.internal.util.Preconditions;
import static stinc.male.sandbox.ratmex.internal.util.Preconditions.checkNotNull;

abstract class AbstractRingBufferRateMeter<S, C extends ConcurrentRingBufferRateMeterConfig>
    extends AbstractRateMeter<S, C> {
  private final boolean sequential;
  private final LongArray samplesHistory;//the length of this array is multiple of the historyLength
  private final long samplesWindowStepNanos;//essentially timeSensitivityNanos
  private final int maxTicksCountAttempts;
  @Nullable
  private final StampedLock ticksCountLock;//we don't need an analogous field for a sequential implementation
  /*Same length as samples history;
    required to overcome a problem which arises when we move the samples window too far while we are registering a new sample.
    A smaller number of locks can be used in the future via a technique similar to lock striping.*/
  @Nullable
  private final LockStrategy[] ticksCountLocks;//we don't need an analogous field for a sequential implementation
  @Nullable
  private final AtomicLong atomicSamplesWindowShiftSteps;//samplesWindowShiftSteps for a concurrent implementation
  private long samplesWindowShiftSteps;//for a sequential implementation
  @Nullable
  private final AtomicLong atomicCompletedSamplesWindowShiftSteps;//we don't need an analogous field for a sequential implementation
  @Nullable
  private final WaitStrategy completedSamplesWindowShiftStepsWaitStrategy;//we don't need an analogous field for a sequential implementation

  /**
   * @param startNanos A {@linkplain #getStartNanos() starting point} that is used to calculate elapsed time in nanoseconds (tNanos).
   * @param samplesInterval A size of the {@linkplain #getSamplesInterval() samples window}.
   * Must not be null, see {@link RateMeter} for valid values.
   * @param config An additional {@linkplain #getConfig() configuration}. Must not be null.
   * @param samplesHistorySupplier A supplier providing an object that will be used to store and access the samples history.
   * The {@link LongArray} provided by this supplier must be thread-safe if {@code sequential} argument is false.
   * @param sequential Specifies whether the {@link RateMeter} must be thread-safe (will be used concurrently, so the value is false),
   * or not (will be used sequentially, so the value is true).
   */
  AbstractRingBufferRateMeter(
      final long startNanos,
      final Duration samplesInterval,
      final C config,
      final Function<Integer, ? extends LongArray> samplesHistorySupplier,
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
            samplesIntervalNanos, timeSensitivityNanos));
    samplesWindowStepNanos = samplesIntervalNanos / samplesIntervalArrayLength;
    samplesHistory = samplesHistorySupplier.apply(config.getHistoryLength() * samplesIntervalArrayLength);
    maxTicksCountAttempts = getConfig().getMaxTicksCountAttempts() < 3 ? 3 : getConfig().getMaxTicksCountAttempts();
    this.sequential = sequential;
    if (sequential) {
      ticksCountLock = null;
      ticksCountLocks = null;
      atomicSamplesWindowShiftSteps = null;
      samplesWindowShiftSteps = 0;
      atomicCompletedSamplesWindowShiftSteps = null;
      completedSamplesWindowShiftStepsWaitStrategy = null;
    } else {
      ticksCountLock = new StampedLock();
      if (config.isStrictTick()) {
        ticksCountLocks = new LockStrategy[samplesHistory.length()];
        for (int idx = 0; idx < ticksCountLocks.length; idx++) {
          ticksCountLocks[idx] = config.getLockStrategySupplier()
              .get();
        }
      } else {
        ticksCountLocks = null;
      }
      atomicSamplesWindowShiftSteps = new AtomicLong();
      samplesWindowShiftSteps = 0;
      atomicCompletedSamplesWindowShiftSteps = new AtomicLong();
      completedSamplesWindowShiftStepsWaitStrategy = config.getWaitStrategySupplier()
          .get();
    }
  }

  @Override
  public final long rightSamplesWindowBoundary() {
    final long result;
    if (sequential) {
      result = rightSamplesWindowBoundary(samplesWindowShiftSteps);
    } else {
      assert atomicSamplesWindowShiftSteps != null;
      result = rightSamplesWindowBoundary(atomicSamplesWindowShiftSteps.get());
    }
    return result;
  }

  /*The implementation of this method is an exact copy of ticksCount(RateMeterReading) except for lines related to RateMeterReading.
    This is bad, but I don't see any other way to implement both methods in a garbage-free way.*/
  @Override
  public final long ticksCount() {
    long value = 0;
    boolean readingDone = false;
    long samplesWindowShiftSteps;
    if (sequential) {
      samplesWindowShiftSteps = this.samplesWindowShiftSteps;
      for (int idx = leftSamplesWindowIdx(samplesWindowShiftSteps), i = 0;
          i < samplesHistory.length() / getConfig().getHistoryLength();
          idx = nextSamplesWindowIdx(idx), i++) {
        value += samplesHistory.get(idx);
      }
      readingDone = true;
    } else {
      assert atomicSamplesWindowShiftSteps != null;
      assert ticksCountLock != null;
      long ticksCountReadLockStamp = 0;
      try {
        samplesWindowShiftSteps = atomicSamplesWindowShiftSteps.get();
        for (int ri = 0; ri < Integer.MAX_VALUE; ri++) {//if the number of tick threads is finite (should be true), then this loop successfully stops
          waitForCompletedWindowShiftSteps(samplesWindowShiftSteps);
          final int leftSamplesWindowIdx = leftSamplesWindowIdx(samplesWindowShiftSteps);
          for (int idx = leftSamplesWindowIdx, i = 0;
              i < samplesHistory.length() / getConfig().getHistoryLength();
              idx = nextSamplesWindowIdx(idx), i++) {
            value += samplesHistory.get(idx);
          }
          readingDone = true;
          final long newSamplesWindowShiftSteps = atomicSamplesWindowShiftSteps.get();
          if (newSamplesWindowShiftSteps - samplesWindowShiftSteps <=
              samplesHistory.length() - samplesHistory.length() / getConfig().getHistoryLength()) {
            //the samples window may has been moved while we were counting, but result is still correct
            break;
          } else {//the samples window has been moved too far
            samplesWindowShiftSteps = newSamplesWindowShiftSteps;
            /*We acquire the read lock to prevent concurrently running tick methods from moving the samples window too far.
              However since tick method acquires the write lock not always, but only if sees the read lock acquired,
              there is a race condition which still may lead to the samples window being moved,
              though the likelihood of such situation is now much less.*/
            if (ticksCountReadLockStamp == 0 && ri >= maxTicksCountAttempts / 2) {
              //we have spent half of the read attempts, let us fall over to lock approach
              ticksCountReadLockStamp = ticksCountLock.readLock();
            }
          }
        }
      } finally {
        if (ticksCountReadLockStamp != 0) {
          ticksCountLock.unlockRead(ticksCountReadLockStamp);
        }
      }
    }
    assert readingDone;
    return value;
  }

  /**
   * @return {@inheritDoc}
   * The reading is always {@linkplain RateMeterReading#isAccurate() accurate}.
   */
  @Override
  public final RateMeterReading ticksCount(final RateMeterReading reading) {
    checkNotNull(reading, "reading");
    reading.setAccurate(true);
    boolean readingDone = false;
    long samplesWindowShiftSteps;
    if (sequential) {
      long value = 0;
      samplesWindowShiftSteps = this.samplesWindowShiftSteps;
      for (int idx = leftSamplesWindowIdx(samplesWindowShiftSteps), i = 0;
          i < samplesHistory.length() / getConfig().getHistoryLength();
          idx = nextSamplesWindowIdx(idx), i++) {
        value += samplesHistory.get(idx);
      }
      reading.setValue(value);
      readingDone = true;
    } else {
      assert atomicSamplesWindowShiftSteps != null;
      assert ticksCountLock != null;
      long ticksCountReadLockStamp = 0;
      try {
        samplesWindowShiftSteps = atomicSamplesWindowShiftSteps.get();
        for (int ri = 0; ri < Integer.MAX_VALUE; ri++) {//if the number of tick threads is finite (should be true), then this loop successfully stops
          long value = 0;
          waitForCompletedWindowShiftSteps(samplesWindowShiftSteps);
          final int leftSamplesWindowIdx = leftSamplesWindowIdx(samplesWindowShiftSteps);
          for (int idx = leftSamplesWindowIdx, i = 0;
              i < samplesHistory.length() / getConfig().getHistoryLength();
              idx = nextSamplesWindowIdx(idx), i++) {
            value += samplesHistory.get(idx);
          }
          reading.setValue(value);
          readingDone = true;
          final long newSamplesWindowShiftSteps = atomicSamplesWindowShiftSteps.get();
          if (newSamplesWindowShiftSteps - samplesWindowShiftSteps <=
              samplesHistory.length() - samplesHistory.length() / getConfig().getHistoryLength()) {
            //the samples window may has been moved while we were counting, but result is still correct
            break;
          } else {//the samples window has been moved too far
            samplesWindowShiftSteps = newSamplesWindowShiftSteps;
            /*We acquire the read lock to prevent concurrently running tick methods from moving the samples window too far.
              However since tick method acquires the write lock not always, but only if sees the read lock acquired,
              there is a race condition which still may lead to the samples window being moved,
              though the likelihood of such situation is now much less.*/
            if (ticksCountReadLockStamp == 0 && ri >= maxTicksCountAttempts / 2) {
              //we have spent half of the read attempts, let us fall over to lock approach
              ticksCountReadLockStamp = ticksCountLock.readLock();
            }
          }
        }
      } finally {
        if (ticksCountReadLockStamp != 0) {
          ticksCountLock.unlockRead(ticksCountReadLockStamp);
        }
      }
    }
    assert readingDone;
    reading.setTNanos(rightSamplesWindowBoundary(samplesWindowShiftSteps));
    return reading;
  }

  @Override
  public final void tick(final long count, final long tNanos) {
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
      if (samplesWindowShiftSteps - samplesHistory.length() < targetSamplesWindowShiftSteps) {//tNanos is ahead of or within the samples history
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
          assert ticksCountLock != null;
          assert atomicCompletedSamplesWindowShiftSteps != null;
          boolean moved = false;
          long ticksCountWriteLockStamp = 0;
          while (samplesWindowShiftSteps < targetSamplesWindowShiftSteps) {//move the samples window if we need to
            if (ticksCountWriteLockStamp == 0) {
              ticksCountWriteLockStamp = ticksCountLock.isReadLocked() ? ticksCountLock.writeLock() : 0;
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
              ticksCountLock.unlockWrite(ticksCountWriteLockStamp);
            }
          }
        }
      }
      getTicksTotalCounter().add(count);
    }
  }

  @Override
  public final double rateAverage(final long tNanos) {
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

  /*The implementation of this method is an exact copy of rate(long, RateMeterReading) except for lines related to RateMeterReading.
    This is bad, but I don't see any other way to implement both methods in a garbage-free way.*/
  @Override
  public final double rate(final long tNanos) {
    checkArgument(tNanos, "tNanos");
    final double value;
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
      value = ConversionsAndChecks.rateAverage(//this is the same as rateAverage()
          rightNanos,
          samplesIntervalNanos,
          getStartNanos(),
          ticksTotalCount());
    } else {//tNanos is within or ahead of the samples window
      final long effectiveLeftNanos = tNanos - samplesIntervalNanos;
      if (NanosComparator.compare(rightNanos, effectiveLeftNanos) <= 0) {
        //tNanos is way too ahead of the samples window and there are no samples for the requested tNanos
        value = 0;
      } else {
        final long effectiveRightNanos = NanosComparator.compare(tNanos, rightNanos) <= 0
            ? tNanos//tNanos is within the samples window
            : rightNanos;//tNanos is ahead of samples window
        final long count = count(effectiveLeftNanos, effectiveRightNanos);
        if (sequential) {
          value = count;
        } else {
          final long tNanosSamplesWindowShiftSteps = samplesWindowShiftSteps(tNanos);
          long newSamplesWindowShiftSteps = atomicSamplesWindowShiftSteps.get();
          if (newSamplesWindowShiftSteps - tNanosSamplesWindowShiftSteps <= samplesHistory.length()) {
            //the samples window may has been moved while we were counting, but count is still correct
            value = count;
          } else {//the samples window has been moved too far, return average
            final long newRightNanos = rightSamplesWindowBoundary(newSamplesWindowShiftSteps);
            value = ConversionsAndChecks.rateAverage(//this is the same as rateAverage()
                newRightNanos, samplesIntervalNanos, getStartNanos(), ticksTotalCount());
          }
        }
      }
    }
    return value;
  }

  /**
   * @return {@inheritDoc}
   * The reading is not {@linkplain RateMeterReading#isAccurate() accurate} in cases when the method returns {@link #rateAverage()}.
   */
  @Override
  public final RateMeterReading rate(final long tNanos, final RateMeterReading reading) {
    checkArgument(tNanos, "tNanos");
    checkNotNull(reading, "reading");
    reading.setTNanos(tNanos);
    reading.setAccurate(true);
    final boolean readingDone;
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
      final double value = ConversionsAndChecks.rateAverage(//this is the same as rateAverage()
          rightNanos,
          samplesIntervalNanos,
          getStartNanos(),
          ticksTotalCount());
      reading.setValue(value);
      readingDone = true;
    } else {//tNanos is within or ahead of the samples window
      final long effectiveLeftNanos = tNanos - samplesIntervalNanos;
      if (NanosComparator.compare(rightNanos, effectiveLeftNanos) <= 0) {
        //tNanos is way too ahead of the samples window and there are no samples for the requested tNanos
        reading.setValue(0);
        readingDone = true;
      } else {
        final long effectiveRightNanos = NanosComparator.compare(tNanos, rightNanos) <= 0
            ? tNanos//tNanos is within the samples window
            : rightNanos;//tNanos is ahead of samples window
        final long count = count(effectiveLeftNanos, effectiveRightNanos);
        if (sequential) {
          reading.setValue(count);
          readingDone = true;
        } else {
          final long tNanosSamplesWindowShiftSteps = samplesWindowShiftSteps(tNanos);
          long newSamplesWindowShiftSteps = atomicSamplesWindowShiftSteps.get();
          if (newSamplesWindowShiftSteps - tNanosSamplesWindowShiftSteps <= samplesHistory.length()) {
            //the samples window may has been moved while we were counting, but count is still correct
            reading.setValue(count);
            readingDone = true;
          } else {//the samples window has been moved too far, return average
            final long newRightNanos = rightSamplesWindowBoundary(newSamplesWindowShiftSteps);
            reading.setTNanos(newRightNanos);
            reading.setAccurate(false);
            final double value = ConversionsAndChecks.rateAverage(//this is the same as rateAverage()
                newRightNanos, samplesIntervalNanos, getStartNanos(), ticksTotalCount());
            reading.setValue(value);
            readingDone = true;
          }
        }
      }
    }
    assert readingDone;
    return reading;
  }

  /**
   * This method is called by {@link #tick(long, long)} not more than once per invocation of {@link #tick(long, long)}
   * when it fails to correctly register ticks.
   * Such a failure can only happen if this object is not thread-safe
   * (see {@link #AbstractRingBufferRateMeter(long, Duration, ConcurrentRingBufferRateMeterConfig, Function, boolean)})
   * and {@link ConcurrentRingBufferRateMeterConfig#isStrictTick()} is false.
   */
  protected void registerFailedAccuracyEventForTick() {
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

  private final void tickAccumulateSample(final int targetIdx, final long count, final long targetSamplesWindowShiftSteps) {
    if (sequential) {
      samplesHistory.add(targetIdx, count);
    } else {
      assert atomicSamplesWindowShiftSteps != null;
      if (ticksCountLocks == null) {//not strict mode, no locking
        samplesHistory.add(targetIdx, count);
        final long samplesWindowShiftSteps = atomicSamplesWindowShiftSteps.get();
        if (targetSamplesWindowShiftSteps < samplesWindowShiftSteps - samplesHistory.length()) {
          //we could have registered (but it is not necessary) ticks at an incorrect instant because samples window had been moved too far
          registerFailedAccuracyEventForTick();
        }
      } else {
        final long stamp = sharedLockTicksCount(targetIdx);
        try {
          final long samplesWindowShiftSteps = atomicSamplesWindowShiftSteps.get();
          if (samplesWindowShiftSteps - samplesHistory.length() < targetSamplesWindowShiftSteps) {
            //double check that tNanos is still within the samples history
            samplesHistory.add(targetIdx, count);
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
    if (NanosComparator.compare(fromInclusiveNanos, toInclusiveNanos) <= 0) {//fix for AbstractRateMeterUnitTest.rateHanging2
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

  private final long rightSamplesWindowBoundary(final long samplesWindowShiftSteps) {
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
    return result;
  }

  private final int leftSamplesWindowIdx(final long samplesWindowShiftSteps) {
    //the result can not be greater than samples.length, which is int, so it is a safe cast to int
    return (int)((samplesWindowShiftSteps + samplesHistory.length() - samplesHistory.length() / getConfig().getHistoryLength()) %
        samplesHistory.length());
  }

  private final int rightSamplesWindowIdx(final long samplesWindowShiftSteps) {
    //the result can not be greater than samples.length, which is int, so it is a safe cast to int
    return (int)((samplesWindowShiftSteps + samplesHistory.length() - 1) % samplesHistory.length());
  }

  private final int nextSamplesWindowIdx(final int idx) {
    return (idx + 1) % samplesHistory.length();
  }
}