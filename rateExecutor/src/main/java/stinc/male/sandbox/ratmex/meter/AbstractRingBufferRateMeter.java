package stinc.male.sandbox.ratmex.meter;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import javax.annotation.Nullable;
import stinc.male.sandbox.ratmex.internal.util.ConversionsAndChecks;
import stinc.male.sandbox.ratmex.internal.util.Preconditions;
import static stinc.male.sandbox.ratmex.internal.util.Preconditions.checkNotNull;

//TODO make public, methods not final, document
abstract class AbstractRingBufferRateMeter<S, C extends ConcurrentRateMeterConfig>
    extends AbstractRateMeter<S, C> {
  private final boolean sequential;
  private final LongArray samplesHistory;//the length of this array is multiple of the historyLength
  private final long samplesWindowStepNanos;//essentially timeSensitivityNanos
  private final int maxTicksCountAttempts;
  @Nullable
  private final LockStrategy ticksCountLock;//we don't need an analogous field for a sequential implementation
  @Nullable
  private final LockStrategy ticksResetLock;//we don't need an analogous field for a sequential implementation
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
    Preconditions.checkArgument(getTimeSensitivityNanos() <= getSamplesIntervalNanos(), "config",
        () -> String.format("timeSensitivity=%sns must be less than or equals to samplesIntervalNanos=%sns",
            getTimeSensitivityNanos(), getSamplesIntervalNanos()));
    Preconditions.checkArgument(getSamplesIntervalNanos() % getTimeSensitivityNanos() == 0,
        "samplesInterval", () -> String.format("The specified samplesInterval=%sns and timeSensitivity=%sns " +
                "can not be used together because samplesInterval is not a multiple of timeSensitivity",
            getSamplesIntervalNanos(), getTimeSensitivityNanos()));
    final int samplesIntervalArrayLength = (int)(getSamplesIntervalNanos() / getTimeSensitivityNanos());
    Preconditions.checkArgument(getSamplesIntervalNanos() == samplesIntervalArrayLength * getTimeSensitivityNanos(), "samplesInterval",
        () -> String.format("The ratio of the specified samplesInterval=%sns and timeSensitivity=%sns is too high",
            getSamplesIntervalNanos(), getTimeSensitivityNanos()));
    samplesWindowStepNanos = getSamplesIntervalNanos() / samplesIntervalArrayLength;
    samplesHistory = samplesHistorySupplier.apply(config.getHistoryLength() * samplesIntervalArrayLength);
    assert samplesWindowStepNanos == getTimeSensitivityNanos();
    assert getSamplesIntervalNanos() * config.getHistoryLength() == samplesHistory.length() * samplesWindowStepNanos;
    maxTicksCountAttempts = getConfig().getMaxTicksCountAttempts() < 3 ? 3 : getConfig().getMaxTicksCountAttempts();
    this.sequential = sequential;
    if (sequential) {
      ticksCountLock = null;
      ticksResetLock = null;
      atomicSamplesWindowShiftSteps = null;
      samplesWindowShiftSteps = 0;
      atomicCompletedSamplesWindowShiftSteps = null;
      completedSamplesWindowShiftStepsWaitStrategy = null;
    } else {
      ticksCountLock = config.getLockStrategySupplier()
          .get();
      if (config.isStrictTick()) {
        ticksResetLock = config.getLockStrategySupplier()
            .get();
      } else {
        ticksResetLock = null;
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
      assert atomicCompletedSamplesWindowShiftSteps != null;
      result = rightSamplesWindowBoundary(atomicCompletedSamplesWindowShiftSteps.get());
    }
    return result;
  }

  /*The implementation of this method is an exact copy of ticksCount(RateMeterReading) except for lines related to RateMeterReading.
    This is bad, but I don't see any other way to implement both methods in a garbage-free way.*/
  @Override
  public final long ticksCount() {
    final long value;
    final int stepsCountInSamplesWindow = samplesHistory.length() / getConfig().getHistoryLength();
    long samplesWindowShiftSteps;
    if (sequential) {
      samplesWindowShiftSteps = this.samplesWindowShiftSteps;
      value = countBasedOnIdx(leftSamplesWindowIdx(samplesWindowShiftSteps), stepsCountInSamplesWindow);
    } else {
      assert atomicSamplesWindowShiftSteps != null;
      assert atomicCompletedSamplesWindowShiftSteps != null;
      assert ticksCountLock != null;
      long ticksCountReadLockStamp = 0;
      try {
        final long maxTolerableShiftSteps = samplesHistory.length() - stepsCountInSamplesWindow;
        samplesWindowShiftSteps = atomicCompletedSamplesWindowShiftSteps.get();
        assert samplesWindowShiftSteps <= atomicSamplesWindowShiftSteps.get();
        int readIteration = 0;
        int trySharedLockAttempts = 1;
        while (true) {//if the number of tick threads is finite (should be true), then this loop successfully stops
          long count = countBasedOnIdx(leftSamplesWindowIdx(samplesWindowShiftSteps), stepsCountInSamplesWindow);
          if (atomicSamplesWindowShiftSteps.get() - samplesWindowShiftSteps <= maxTolerableShiftSteps) {
            //the samples window may has been moved while we were counting, but value is still correct
            value = count;
            break;
          } else {//the samples window has been moved too far
            samplesWindowShiftSteps = atomicCompletedSamplesWindowShiftSteps.get();
            /*We acquire the read lock to prevent concurrently running tick methods from moving the samples window too far.
              However since tick method acquires the write lock not always, but only if sees the read lock acquired,
              there is a race condition which still may lead to the samples window being moved,
              though the likelihood of such situation is now much less.*/
            if (ticksCountReadLockStamp == 0 && readIteration >= maxTicksCountAttempts / 2) {
              //we have spent half of the read attempts, let us fall over to lock approach
              if (trySharedLockAttempts > 0) {
                trySharedLockAttempts--;
                ticksCountReadLockStamp = ticksCountLock.trySharedLock();
              } else {
                ticksCountReadLockStamp = ticksCountLock.sharedLock();
              }
            }
          }
          readIteration = Math.addExact(readIteration, 1);
        }
      } finally {
        if (ticksCountReadLockStamp != 0) {
          ticksCountLock.unlockShared(ticksCountReadLockStamp);
        }
      }
    }
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
    final boolean readingDone;
    final int stepsCountInSamplesWindow = samplesHistory.length() / getConfig().getHistoryLength();
    long samplesWindowShiftSteps;
    if (sequential) {
      samplesWindowShiftSteps = this.samplesWindowShiftSteps;
      reading.setValue(countBasedOnIdx(leftSamplesWindowIdx(samplesWindowShiftSteps), stepsCountInSamplesWindow));
      readingDone = true;
    } else {
      assert atomicSamplesWindowShiftSteps != null;
      assert atomicCompletedSamplesWindowShiftSteps != null;
      assert ticksCountLock != null;
      long ticksCountReadLockStamp = 0;
      try {
        final long maxTolerableShiftSteps = samplesHistory.length() - stepsCountInSamplesWindow;
        samplesWindowShiftSteps = atomicCompletedSamplesWindowShiftSteps.get();
        assert samplesWindowShiftSteps <= atomicSamplesWindowShiftSteps.get();
        int readIteration = 0;
        int trySharedLockAttempts = 1;
        while (true) {//if the number of tick threads is finite (should be true), then this loop successfully stops
          long count = countBasedOnIdx(leftSamplesWindowIdx(samplesWindowShiftSteps), stepsCountInSamplesWindow);
          if (atomicSamplesWindowShiftSteps.get() - samplesWindowShiftSteps <= maxTolerableShiftSteps) {
            //the samples window may has been moved while we were counting, but value is still correct
            reading.setValue(count);
            readingDone = true;
            break;
          } else {//the samples window has been moved too far
            samplesWindowShiftSteps = atomicCompletedSamplesWindowShiftSteps.get();
            /*We acquire the read lock to prevent concurrently running tick methods from moving the samples window too far.
              However since tick method acquires the write lock not always, but only if sees the read lock acquired,
              there is a race condition which still may lead to the samples window being moved,
              though the likelihood of such situation is now much less.*/
            if (ticksCountReadLockStamp == 0 && readIteration >= maxTicksCountAttempts / 2) {
              //we have spent half of the read attempts, let us fall over to lock approach
              if (trySharedLockAttempts > 0) {
                trySharedLockAttempts--;
                ticksCountReadLockStamp = ticksCountLock.trySharedLock();
              } else {
                ticksCountReadLockStamp = ticksCountLock.sharedLock();
              }
            }
          }
          readIteration = Math.addExact(readIteration, 1);
        }
      } finally {
        if (ticksCountReadLockStamp != 0) {
          ticksCountLock.unlockShared(ticksCountReadLockStamp);
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
            if (ticksCountWriteLockStamp == 0 && ticksCountLock.isSharedLocked()) {
              ticksCountWriteLockStamp = ticksCountLock.lock();
            }
            moved = atomicSamplesWindowShiftSteps.compareAndSet(samplesWindowShiftSteps, targetSamplesWindowShiftSteps);
            if (moved) {
              break;
            } else {//another thread has moved the samples window
              samplesWindowShiftSteps = atomicSamplesWindowShiftSteps.get();
            }
          }
          try {
            final int targetIdx = rightSamplesWindowIdx(targetSamplesWindowShiftSteps);
            if (moved) {
              //samplesWindowShiftSteps currently represents value right before we moved the samples window
              assert samplesWindowShiftSteps < targetSamplesWindowShiftSteps;
              //numberOfSteps is by how many steps we have moved the samples window
              final long numberOfSteps = targetSamplesWindowShiftSteps - samplesWindowShiftSteps;
              waitForCompletedWindowShiftSteps(samplesWindowShiftSteps);//"serializing waiting condition"
              /*We are going to reset some or all samples because we need to reuse them (this is a ring buffer).
                Note that no other threads can concurrently reset samples because they are waiting on the "serializing waiting condition",
                which can be found above.*/
              if (numberOfSteps < samplesHistory.length()) {//reset some (not all) samples
                for (int idx = nextSamplesWindowIdx(rightSamplesWindowIdx(samplesWindowShiftSteps)), i = 0;
                    i < numberOfSteps && i < samplesHistory.length();
                    idx = nextSamplesWindowIdx(idx), i++) {
                  tickResetSample(idx, idx == targetIdx ? count : 0);
                }
              } else {//reset all samples
                for (int idx = 0; idx < samplesHistory.length(); idx++) {
                  tickResetSample(idx, idx == targetIdx ? count : 0);
                }
              }
              long completedSamplesWindowShiftSteps = atomicCompletedSamplesWindowShiftSteps.get();
              assert samplesWindowShiftSteps == completedSamplesWindowShiftSteps;
              atomicCompletedSamplesWindowShiftSteps.set(targetSamplesWindowShiftSteps);//complete steps up to the targetSamplesWindowShiftSteps
            } else {
              waitForCompletedWindowShiftSteps(targetSamplesWindowShiftSteps);
              tickAccumulateSample(targetIdx, count, targetSamplesWindowShiftSteps);
            }
          } finally {
            if (ticksCountWriteLockStamp != 0) {
              ticksCountLock.unlock(ticksCountWriteLockStamp);
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
    double value;
    final boolean readingDone;
    final long samplesIntervalNanos = getSamplesIntervalNanos();
    long completedSamplesWindowShiftSteps;
    final long samplesWindowShiftSteps;
    if (sequential) {
      completedSamplesWindowShiftSteps = this.samplesWindowShiftSteps;
      samplesWindowShiftSteps = completedSamplesWindowShiftSteps;
    } else {
      assert atomicSamplesWindowShiftSteps != null;
      assert atomicCompletedSamplesWindowShiftSteps != null;
      completedSamplesWindowShiftSteps = atomicCompletedSamplesWindowShiftSteps.get();
      samplesWindowShiftSteps = atomicSamplesWindowShiftSteps.get();
    }
    assert completedSamplesWindowShiftSteps <= samplesWindowShiftSteps;
    long completedRightNanos = rightSamplesWindowBoundary(completedSamplesWindowShiftSteps);//same as rightSamplesWindowBoundary()
    final long rightNanos = rightSamplesWindowBoundary(samplesWindowShiftSteps);
    final long leftNanos = rightNanos - samplesIntervalNanos;
    final long effectiveLeftNanos = tNanos - samplesIntervalNanos;
    final long historyLengthNanos = samplesHistory.length() * samplesWindowStepNanos;
    final long leftmostHistoryNanos = rightNanos - historyLengthNanos;
    if (NanosComparator.compare(effectiveLeftNanos, leftmostHistoryNanos) < 0) {
      //tNanos is behind the samples window, so return average over all samples
      value = ConversionsAndChecks.rateAverage(//this is the same as rateAverage()
          completedRightNanos, samplesIntervalNanos, getStartNanos(), ticksTotalCount());
      readingDone = true;
    } else {//tNanos is within or ahead of the samples window
      if (NanosComparator.compare(completedRightNanos, effectiveLeftNanos) <= 0) {
        //tNanos is way too ahead of the completed part of the samples window and there are no samples for the requested tNanos
        value = 0;
        readingDone = true;
      } else {
        final long effectiveRightNanos = NanosComparator.compare(tNanos, completedRightNanos) <= 0
            ? tNanos//tNanos is within the the completed part of the samples window
            : completedRightNanos;//tNanos is ahead of the completed part of the samples window
        final long count = countBasedOnNanos(effectiveLeftNanos, effectiveRightNanos);
        if (sequential) {
          value = count;
          readingDone = true;
        } else {
          final long newRightNanos = rightSamplesWindowBoundary(atomicSamplesWindowShiftSteps.get());
          final long newLeftmostHistoryNanos = newRightNanos - historyLengthNanos;
          if (NanosComparator.compare(newLeftmostHistoryNanos, effectiveLeftNanos) <= 0) {
            //the samples window may has been moved while we were counting, but value is still correct
            value = count;
            readingDone = true;
          } else {//the samples window has been moved too far, return average
            completedSamplesWindowShiftSteps = atomicCompletedSamplesWindowShiftSteps.get();
            completedRightNanos = rightSamplesWindowBoundary(completedSamplesWindowShiftSteps);
            value = ConversionsAndChecks.rateAverage(//this is the same as rateAverage()
                completedRightNanos, samplesIntervalNanos, getStartNanos(), ticksTotalCount());
            readingDone = true;
          }
        }
      }
    }
    assert readingDone;
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
    long completedSamplesWindowShiftSteps;
    final long samplesWindowShiftSteps;
    if (sequential) {
      completedSamplesWindowShiftSteps = this.samplesWindowShiftSteps;
      samplesWindowShiftSteps = completedSamplesWindowShiftSteps;
    } else {
      assert atomicSamplesWindowShiftSteps != null;
      assert atomicCompletedSamplesWindowShiftSteps != null;
      completedSamplesWindowShiftSteps = atomicCompletedSamplesWindowShiftSteps.get();
      samplesWindowShiftSteps = atomicSamplesWindowShiftSteps.get();
    }
    assert completedSamplesWindowShiftSteps <= samplesWindowShiftSteps;
    long completedRightNanos = rightSamplesWindowBoundary(completedSamplesWindowShiftSteps);//same as rightSamplesWindowBoundary()
    final long rightNanos = rightSamplesWindowBoundary(samplesWindowShiftSteps);
    final long leftNanos = rightNanos - samplesIntervalNanos;
    final long effectiveLeftNanos = tNanos - samplesIntervalNanos;
    final long historyLengthNanos = samplesHistory.length() * samplesWindowStepNanos;
    final long leftmostHistoryNanos = rightNanos - historyLengthNanos;
    if (NanosComparator.compare(effectiveLeftNanos, leftmostHistoryNanos) < 0) {
      //tNanos is behind the samples window, so return average over all samples
      reading.setTNanos(completedRightNanos);
      reading.setAccurate(false);
      final double value = ConversionsAndChecks.rateAverage(//this is the same as rateAverage()
          completedRightNanos, samplesIntervalNanos, getStartNanos(), ticksTotalCount());
      reading.setValue(value);
      readingDone = true;
    } else {//tNanos is within or ahead of the samples window
      if (NanosComparator.compare(completedRightNanos, effectiveLeftNanos) <= 0) {
        //tNanos is way too ahead of the completed part of the samples window and there are no samples for the requested tNanos
        reading.setValue(0);
        readingDone = true;
      } else {
        final long effectiveRightNanos = NanosComparator.compare(tNanos, completedRightNanos) <= 0
            ? tNanos//tNanos is within the the completed part of the samples window
            : completedRightNanos;//tNanos is ahead of the completed part of the samples window
        final long count = countBasedOnNanos(effectiveLeftNanos, effectiveRightNanos);
        if (sequential) {
          reading.setValue(count);
          readingDone = true;
        } else {
          final long newRightNanos = rightSamplesWindowBoundary(atomicSamplesWindowShiftSteps.get());
          final long newLeftmostHistoryNanos = newRightNanos - historyLengthNanos;
          if (NanosComparator.compare(newLeftmostHistoryNanos, effectiveLeftNanos) <= 0) {
            //the samples window may has been moved while we were counting, but value is still correct
            reading.setValue(count);
            readingDone = true;
          } else {//the samples window has been moved too far, return average
            completedSamplesWindowShiftSteps = atomicCompletedSamplesWindowShiftSteps.get();
            completedRightNanos = rightSamplesWindowBoundary(completedSamplesWindowShiftSteps);
            reading.setTNanos(completedRightNanos);
            reading.setAccurate(false);
            final double value = ConversionsAndChecks.rateAverage(//this is the same as rateAverage()
                completedRightNanos, samplesIntervalNanos, getStartNanos(), ticksTotalCount());
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
   * (see {@link #AbstractRingBufferRateMeter(long, Duration, ConcurrentRateMeterConfig, Function, boolean)})
   * and {@link ConcurrentRateMeterConfig#isStrictTick()} is false.
   */
  protected void registerFailedAccuracyEventForTick() {
  }

  private final void tickResetSample(final int idx, final long value) {
    if (ticksResetLock == null) {//either sequential or not strict mode; no locking
      samplesHistory.set(idx, value);
    } else {
      final long ticksResetExclusiveLockStamp = ticksResetLock.lock();
      try {
        samplesHistory.set(idx, value);
      } finally {
        ticksResetLock.unlock(ticksResetExclusiveLockStamp);
      }
    }
  }

  private final void tickAccumulateSample(final int targetIdx, final long count, final long targetSamplesWindowShiftSteps) {
    if (sequential) {
      samplesHistory.add(targetIdx, count);
    } else {
      assert atomicSamplesWindowShiftSteps != null;
      if (ticksResetLock == null) {//not strict mode, no locking
        samplesHistory.add(targetIdx, count);
        final long samplesWindowShiftSteps = atomicSamplesWindowShiftSteps.get();
        if (targetSamplesWindowShiftSteps <= samplesWindowShiftSteps - samplesHistory.length()) {
          //we could have registered (but it is not necessary) ticks at an incorrect instant because samples window have been moved too far
          registerFailedAccuracyEventForTick();
        }
      } else {
        final long ticksResetSharedLockStamp = ticksResetLock.sharedLock();
        try {
          final long samplesWindowShiftSteps = atomicSamplesWindowShiftSteps.get();
          if (samplesWindowShiftSteps - samplesHistory.length() < targetSamplesWindowShiftSteps) {
            //check that targetIdx element has not been reused due to the samples history have been moved too far
            samplesHistory.add(targetIdx, count);
          }
        } finally {
          ticksResetLock.unlockShared(ticksResetSharedLockStamp);
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

  private final long countBasedOnNanos(final long fromExclusiveNanos, final long toInclusiveNanos) {
    final long fromInclusiveNanos = fromExclusiveNanos + samplesWindowStepNanos;//fix for AbstractRateMeterUnitTest.rateBug
    long result = 0;
    if (NanosComparator.compare(fromInclusiveNanos, toInclusiveNanos) <= 0) {//fix for AbstractRateMeterUnitTest.rateHanging2
      final long fromInclusiveSamplesWindowShiftSteps = samplesWindowShiftSteps(fromInclusiveNanos);
      final long toInclusiveSamplesWindowShiftSteps = samplesWindowShiftSteps(toInclusiveNanos);
      assert toInclusiveSamplesWindowShiftSteps - fromInclusiveSamplesWindowShiftSteps + 1 <= samplesHistory.length();
      final int numberOfStepsToCount = (int)(toInclusiveSamplesWindowShiftSteps - fromInclusiveSamplesWindowShiftSteps + 1);
      result = countBasedOnIdx(rightSamplesWindowIdx(fromInclusiveSamplesWindowShiftSteps), numberOfStepsToCount);
    }
    return result;
  }

  private final long countBasedOnIdx(final int fromInclusiveIdx, final int numberOfStepsToCount) {
    long result = 0;
    for (int idx = fromInclusiveIdx, i = 0; i < numberOfStepsToCount; idx = nextSamplesWindowIdx(idx), i++) {
      result += samplesHistory.get(idx);
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