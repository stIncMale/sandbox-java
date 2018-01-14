package stinc.male.sandbox.ratmex.meter;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import javax.annotation.Nullable;
import stinc.male.sandbox.ratmex.NanosComparator;
import stinc.male.sandbox.ratmex.internal.util.ConversionsAndChecks;
import stinc.male.sandbox.ratmex.internal.util.Preconditions;
import static java.lang.Math.addExact;
import static java.lang.Math.min;
import static stinc.male.sandbox.ratmex.internal.util.Preconditions.checkNotNull;
import static stinc.male.sandbox.ratmex.internal.util.Util.format;

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
  private final LockStrategy ticksAccumulateLock;//we don't need an analogous field for a sequential implementation
  @Nullable
  private final AtomicLong atomicSamplesWindowShiftSteps;//write cursor; samplesWindowShiftSteps for a concurrent implementation
  private long samplesWindowShiftSteps;//for a sequential implementation
  private volatile long completedSamplesWindowShiftSteps;//read cursor; for a concurrent implementation
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
        () -> format("timeSensitivity=%sns must be less than or equals to samplesIntervalNanos=%sns",
            getTimeSensitivityNanos(), getSamplesIntervalNanos()));
    Preconditions.checkArgument(getSamplesIntervalNanos() % getTimeSensitivityNanos() == 0,
        "samplesInterval", () -> format("The specified samplesInterval=%sns and timeSensitivity=%sns " +
                "can not be used together because samplesInterval is not a multiple of timeSensitivity",
            getSamplesIntervalNanos(), getTimeSensitivityNanos()));
    final int samplesIntervalArrayLength = (int)(getSamplesIntervalNanos() / getTimeSensitivityNanos());
    Preconditions.checkArgument(getSamplesIntervalNanos() == samplesIntervalArrayLength * getTimeSensitivityNanos(), "samplesInterval",
        () -> format("The ratio of the specified samplesInterval=%sns and timeSensitivity=%sns is too high",
            getSamplesIntervalNanos(), getTimeSensitivityNanos()));
    samplesWindowStepNanos = getSamplesIntervalNanos() / samplesIntervalArrayLength;
    samplesHistory = samplesHistorySupplier.apply(config.getHistoryLength() * samplesIntervalArrayLength);
    assert samplesWindowStepNanos == getTimeSensitivityNanos();
    assert getSamplesIntervalNanos() * config.getHistoryLength() == samplesHistory.length() * samplesWindowStepNanos;
    maxTicksCountAttempts = getConfig().getMaxTicksCountAttempts() < 3 ? 3 : getConfig().getMaxTicksCountAttempts();
    this.sequential = sequential;
    if (sequential) {
      ticksCountLock = null;
      ticksAccumulateLock = null;
      atomicSamplesWindowShiftSteps = null;
      samplesWindowShiftSteps = 0;
      completedSamplesWindowShiftSteps = 0;
      completedSamplesWindowShiftStepsWaitStrategy = null;
    } else {
      ticksCountLock = config.getLockStrategySupplier()
          .get();
      if (config.isStrictTick()) {
        ticksAccumulateLock = config.getLockStrategySupplier()
            .get();
      } else {
        ticksAccumulateLock = null;
      }
      atomicSamplesWindowShiftSteps = new AtomicLong();
      samplesWindowShiftSteps = 0;
      completedSamplesWindowShiftSteps = 0;
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
      result = rightSamplesWindowBoundary(completedSamplesWindowShiftSteps);
    }
    return result;
  }

  /*The implementation of this method is an exact copy of ticksCount(RateMeterReading) except for lines related to RateMeterReading.
    This is bad, but I don't see any other way to implement both methods in a garbage-free way.*/
  @Override
  public final long ticksCount() {
    final long value;
    final int cellsInSamplesWindow = samplesHistory.length() / getConfig().getHistoryLength();
    if (sequential) {
      final long shiftSteps = samplesWindowShiftSteps;
      final long countFromShiftSteps = shiftSteps - cellsInSamplesWindow + 1;
      value = count(samplesHistoryIdx(countFromShiftSteps), cellsInSamplesWindow);
    } else {
      assert atomicSamplesWindowShiftSteps != null;
      assert ticksCountLock != null;
      long ticksCountReadLockStamp = 0;
      try {
        long completedShiftSteps = completedSamplesWindowShiftSteps;
        final long shiftSteps = atomicSamplesWindowShiftSteps.get();
        assert completedShiftSteps <= shiftSteps;
        int readIteration = 0;
        int trySharedLockAttempts = 1;
        while (true) {//if the number of tick threads is finite (which is true), then this loop successfully stops
          final long targetShiftSteps = completedShiftSteps;
          final long countFromShiftSteps = targetShiftSteps - cellsInSamplesWindow + 1;
          final long count = count(samplesHistoryIdx(countFromShiftSteps), cellsInSamplesWindow);
          final long newShiftSteps
              = atomicSamplesWindowShiftSteps.get();//atomicSamplesWindowShiftSteps, not completedSamplesWindowShiftSteps, it's important
          final long minShiftSteps = newShiftSteps - samplesHistory.length() + 1;
          if (minShiftSteps <= countFromShiftSteps) {//the samples history may has been moved while we were counting, but the count is still correct
            value = count;
            break;
          } else {//the samples history has been moved too far
            completedShiftSteps = completedSamplesWindowShiftSteps;
            /*We acquire the read lock to prevent concurrently running tick methods from moving the samples history too far.
              However since tick method acquires the write lock not always, but only when sees the read lock acquired,
              there is a race condition which still may lead to the samples history being moved,
              though the likelihood of such situation is now much less. Hence we still can exceed maxTicksCountAttempts,
              but eventually we are guaranteed to succeed in a final number of attempts*/
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
          readIteration = addExact(readIteration, 1);
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
    reading.setStartNanos(getStartNanos())
        .setUnit(getSamplesInterval())
        .setAccurate(true);
    final boolean readingDone;
    final int cellsInSamplesWindow = samplesHistory.length() / getConfig().getHistoryLength();
    if (sequential) {
      final long shiftSteps = samplesWindowShiftSteps;
      final long countFromShiftSteps = shiftSteps - cellsInSamplesWindow + 1;
      reading.setTNanos(rightSamplesWindowBoundary(shiftSteps))
          .setValue(count(samplesHistoryIdx(countFromShiftSteps), cellsInSamplesWindow));
      readingDone = true;
    } else {
      assert atomicSamplesWindowShiftSteps != null;
      assert ticksCountLock != null;
      long ticksCountReadLockStamp = 0;
      try {
        long completedShiftSteps = completedSamplesWindowShiftSteps;
        final long shiftSteps = atomicSamplesWindowShiftSteps.get();
        assert completedShiftSteps <= shiftSteps;
        int readIteration = 0;
        int trySharedLockAttempts = 1;
        while (true) {//if the number of tick threads is finite (which is true), then this loop successfully stops
          final long targetShiftSteps = completedShiftSteps;
          final long countFromShiftSteps = targetShiftSteps - cellsInSamplesWindow + 1;
          final long count = count(samplesHistoryIdx(countFromShiftSteps), cellsInSamplesWindow);
          final long newShiftSteps
              = atomicSamplesWindowShiftSteps.get();//atomicSamplesWindowShiftSteps, not completedSamplesWindowShiftSteps, it's important
          final long minShiftSteps = newShiftSteps - samplesHistory.length() + 1;
          if (minShiftSteps <= countFromShiftSteps) {//the samples history may has been moved while we were counting, but the count is still correct
            reading.setTNanos(rightSamplesWindowBoundary(targetShiftSteps))
                .setValue(count);
            readingDone = true;
            break;
          } else {//the samples history has been moved too far
            completedShiftSteps = completedSamplesWindowShiftSteps;
            /*We acquire the read lock to prevent concurrently running tick methods from moving the samples history too far.
              However since tick method acquires the write lock not always, but only when sees the read lock acquired,
              there is a race condition which still may lead to the samples history being moved,
              though the likelihood of such situation is now much less. Hence we still can exceed maxTicksCountAttempts,
              but eventually we are guaranteed to succeed in a final number of attempts*/
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
          readIteration = addExact(readIteration, 1);
        }
      } finally {
        if (ticksCountReadLockStamp != 0) {
          ticksCountLock.unlockShared(ticksCountReadLockStamp);
        }
      }
    }
    assert readingDone;
    return reading;
  }

  //TODO create an experimental analogous method and test how many times it can move the window per second
  //TODO compare single/multi thread for very short samples window; for various history lengths
  @Override
  public final void tick(final long count, final long tNanos) {
    checkArgument(tNanos, "tNanos");
    if (count != 0) {
      final long targetShiftSteps = samplesWindowShiftSteps(tNanos);
      long shiftSteps;
      if (sequential) {
        shiftSteps = samplesWindowShiftSteps;
      } else {
        assert atomicSamplesWindowShiftSteps != null;
        shiftSteps = atomicSamplesWindowShiftSteps.get();
      }
      if (shiftSteps - samplesHistory.length() < targetShiftSteps) {//tNanos is ahead of or within the samples history
        if (sequential) {
          final int targetIdx = samplesHistoryIdx(targetShiftSteps);
          if (shiftSteps < targetShiftSteps) {//we need to move the samples window
            samplesWindowShiftSteps = targetShiftSteps;
            final long numberOfSteps = targetShiftSteps - shiftSteps;//numberOfSteps is by how many steps we have moved the samples window
            final int numberOfResetIterations = (int)min(numberOfSteps, samplesHistory.length());//it's safe to cast to int
            for (int i = 0, idx = nextSamplesHistoryIdx(samplesHistoryIdx(shiftSteps));
                i < numberOfResetIterations;
                i++, idx = nextSamplesHistoryIdx(idx)) {//reset moved samples
              samplesHistory.set(idx, idx == targetIdx ? count : 0);
            }
          } else {
            tickAccumulateSample(targetIdx, count, targetShiftSteps);
          }
        } else {
          assert ticksCountLock != null;
          boolean moved = false;
          long ticksCountWriteLockStamp = 0;
          while (shiftSteps < targetShiftSteps) {//move the samples window if we need to
            if (ticksCountWriteLockStamp == 0 && ticksCountLock.isSharedLocked()) {
              ticksCountWriteLockStamp = ticksCountLock.lock();
            }
            //TODO introduce another atomicPreSamplesWindowShiftSteps and not lock for each try to move samples window
            final long ticksResetWriteLockStamp = ticksAccumulateLock == null ? 0 : ticksAccumulateLock.lock();
            try {
              //the only place where we change atomicSamplesWindowShiftSteps
              moved = atomicSamplesWindowShiftSteps.compareAndSet(shiftSteps, targetShiftSteps);
            } finally {
              if (ticksResetWriteLockStamp != 0) {
                ticksAccumulateLock.unlock(ticksResetWriteLockStamp);
              }
            }
            if (moved) {
              break;
            } else {//another thread has moved the samples window
              shiftSteps = atomicSamplesWindowShiftSteps.get();
            }
          }
          try {
            final int targetIdx = samplesHistoryIdx(targetShiftSteps);
            if (moved) {
              assert shiftSteps < targetShiftSteps;
              final long numberOfSteps = targetShiftSteps - shiftSteps;//numberOfSteps is by how many steps we have moved the samples window
              waitForCompletedWindowShiftSteps(shiftSteps);//"serializing waiting condition"
              /*We are going to reset some (or all) samples because we need to reuse them (this is a ring buffer).
                Note that no other threads can concurrently reset samples because they are waiting on the "serializing waiting condition" above.*/
              if (numberOfSteps <= samplesHistory.length()) {//samples history is shifting step by step
                for (int i = 0, idx = nextSamplesHistoryIdx(samplesHistoryIdx(shiftSteps));
                    i < (int)numberOfSteps;//it's safe to cast to int
                    i++, idx = nextSamplesHistoryIdx(idx)) {
                  samplesHistory.set(idx, idx == targetIdx ? count : 0);
                  final long newCompletedShiftSteps = shiftSteps + 1 + i;
                  assert newCompletedShiftSteps == completedSamplesWindowShiftSteps + 1;
                  //this and the below else block are the only places where we change completedSamplesWindowShiftSteps
                  completedSamplesWindowShiftSteps = newCompletedShiftSteps;//complete current step (actually just increment)
                }
              } else {//samples history is shifting with a single leap
                for (int idx = 0; idx < samplesHistory.length(); idx++) {
                  samplesHistory.set(idx, idx == targetIdx ? count : 0);
                }
                assert shiftSteps == completedSamplesWindowShiftSteps;
                //this and the above if block are the only places where we change completedSamplesWindowShiftSteps
                completedSamplesWindowShiftSteps = targetShiftSteps;//complete all steps at once (leap)
              }
            } else {
              waitForCompletedWindowShiftSteps(targetShiftSteps);
              tickAccumulateSample(targetIdx, count, targetShiftSteps);
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
    final long shiftSteps;
    if (sequential) {
      shiftSteps = samplesWindowShiftSteps;
    } else {
      assert atomicSamplesWindowShiftSteps != null;
      shiftSteps = atomicSamplesWindowShiftSteps.get();
    }
    final long rightNanos = rightSamplesWindowBoundary(shiftSteps);
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
    final long completedShiftSteps;
    final long shiftSteps;
    if (sequential) {
      completedShiftSteps = samplesWindowShiftSteps;
      shiftSteps = completedShiftSteps;
    } else {
      assert atomicSamplesWindowShiftSteps != null;
      completedShiftSteps = completedSamplesWindowShiftSteps;
      shiftSteps = atomicSamplesWindowShiftSteps.get();
    }
    assert completedShiftSteps <= shiftSteps;
    final long targetShiftSteps = samplesWindowShiftSteps(tNanos);
    final int cellsInSamplesWindow = samplesHistory.length() / getConfig().getHistoryLength();
    final int cellsInSafeSamplesHistory = samplesHistory.length() - cellsInSamplesWindow;
    final long minSafeShiftSteps = shiftSteps - cellsInSafeSamplesHistory;//shiftSteps, not completedShiftSteps, it's important
    if (targetShiftSteps < minSafeShiftSteps) {//tNanos is behind the safe samples history, so return average over all samples
      final long measuredTNanos = rightSamplesWindowBoundary(shiftSteps);
      value = ConversionsAndChecks.rateAverage(//this is the same as rateAverage()
          measuredTNanos, samplesIntervalNanos, getStartNanos(), ticksTotalCount());
    } else {//tNanos is within or ahead of the safe samples history
      final long countFromShiftSteps = targetShiftSteps - cellsInSamplesWindow + 1;
      if (completedShiftSteps < countFromShiftSteps) {
        //tNanos is way too ahead of the completed part of the safe samples history and there are no samples for the requested tNanos
        value = 0;
      } else {
        assert shiftSteps - samplesHistory.length() + 1 <= countFromShiftSteps;
        final long countToShiftSteps = min(completedShiftSteps, targetShiftSteps);
        assert countFromShiftSteps <= countToShiftSteps && (countToShiftSteps - countFromShiftSteps + 1) <= cellsInSafeSamplesHistory;
        final int numberOfCellsToCount = (int)(countToShiftSteps - countFromShiftSteps) + 1;
        final long count = count(samplesHistoryIdx(countFromShiftSteps), numberOfCellsToCount);
        if (sequential) {
          value = count;
        } else {//check whether safe samples history has been moved too far while we were counting
          final long newShiftSteps
              = atomicSamplesWindowShiftSteps.get();//atomicSamplesWindowShiftSteps, not completedSamplesWindowShiftSteps, it's important
          final long minShiftSteps = newShiftSteps - samplesHistory.length() + 1;
          if (countFromShiftSteps < minShiftSteps) {
            //the safe samples history has been moved too far, so return average over all samples
            final long measuredTNanos = rightSamplesWindowBoundary(shiftSteps);
            value = ConversionsAndChecks.rateAverage(//this is the same as rateAverage()
                measuredTNanos, samplesIntervalNanos, getStartNanos(), ticksTotalCount());
          } else {
            value = count;
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
    reading.setStartNanos(getStartNanos())
        .setUnit(getSamplesInterval())
        .setAccurate(true)
        .setTNanos(tNanos);
    final boolean readingDone;
    final long samplesIntervalNanos = getSamplesIntervalNanos();
    final long completedShiftSteps;
    final long shiftSteps;
    if (sequential) {
      completedShiftSteps = samplesWindowShiftSteps;
      shiftSteps = completedShiftSteps;
    } else {
      assert atomicSamplesWindowShiftSteps != null;
      completedShiftSteps = completedSamplesWindowShiftSteps;
      shiftSteps = atomicSamplesWindowShiftSteps.get();
    }
    assert completedShiftSteps <= shiftSteps;
    final long targetShiftSteps = samplesWindowShiftSteps(tNanos);
    final int cellsInSamplesWindow = samplesHistory.length() / getConfig().getHistoryLength();
    final int cellsInSafeSamplesHistory = samplesHistory.length() - cellsInSamplesWindow;
    final long minSafeShiftSteps = shiftSteps - cellsInSafeSamplesHistory;//shiftSteps, not completedShiftSteps, it's important
    if (targetShiftSteps < minSafeShiftSteps) {//tNanos is behind the safe samples history, so return average over all samples
      final long measuredTNanos = rightSamplesWindowBoundary(shiftSteps);
      final double value = ConversionsAndChecks.rateAverage(//this is the same as rateAverage()
          measuredTNanos, samplesIntervalNanos, getStartNanos(), ticksTotalCount());
      reading.setTNanos(measuredTNanos)
          .setAccurate(false)
          .setValue(value);
      readingDone = true;
    } else {//tNanos is within or ahead of the safe samples history
      final long countFromShiftSteps = targetShiftSteps - cellsInSamplesWindow + 1;
      if (completedShiftSteps < countFromShiftSteps) {
        //tNanos is way too ahead of the completed part of the safe samples history and there are no samples for the requested tNanos
        reading.setValue(0);
        readingDone = true;
      } else {
        assert shiftSteps - samplesHistory.length() + 1 <= countFromShiftSteps;
        final long countToShiftSteps = min(completedShiftSteps, targetShiftSteps);
        assert countFromShiftSteps <= countToShiftSteps && (countToShiftSteps - countFromShiftSteps + 1) <= cellsInSafeSamplesHistory;
        final int numberOfCellsToCount = (int)(countToShiftSteps - countFromShiftSteps) + 1;
        final long count = count(samplesHistoryIdx(countFromShiftSteps), numberOfCellsToCount);
        if (sequential) {
          reading.setValue(count);
          readingDone = true;
        } else {//check whether safe samples history has been moved too far while we were counting
          final long newShiftSteps
              = atomicSamplesWindowShiftSteps.get();//atomicSamplesWindowShiftSteps, not completedSamplesWindowShiftSteps, it's important
          final long minShiftSteps = newShiftSteps - samplesHistory.length() + 1;
          if (countFromShiftSteps < minShiftSteps) {
            //the safe samples history has been moved too far, so return average over all samples
            final long measuredTNanos = rightSamplesWindowBoundary(shiftSteps);
            final double value = ConversionsAndChecks.rateAverage(//this is the same as rateAverage()
                measuredTNanos, samplesIntervalNanos, getStartNanos(), ticksTotalCount());
            reading.setTNanos(measuredTNanos)
                .setAccurate(false)
                .setValue(value);
            readingDone = true;
          } else {
            reading.setValue(count);
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

  private final void tickAccumulateSample(final int targetIdx, final long count, final long targetSamplesWindowShiftSteps) {
    if (sequential) {
      samplesHistory.add(targetIdx, count);
    } else {
      assert atomicSamplesWindowShiftSteps != null;
      if (ticksAccumulateLock == null) {//not strict mode, no locking
        samplesHistory.add(targetIdx, count);
        final long shiftSteps = atomicSamplesWindowShiftSteps.get();
        if (targetSamplesWindowShiftSteps <= shiftSteps - samplesHistory.length()) {
          //we could have registered (but it is not necessary) ticks at an incorrect instant because samples history have been moved too far
          registerFailedAccuracyEventForTick();
        }
      } else {
        final long ticksResetSharedLockStamp = ticksAccumulateLock.sharedLock();
        try {
          final long shiftSteps = atomicSamplesWindowShiftSteps.get();
          if (shiftSteps - samplesHistory.length() < targetSamplesWindowShiftSteps) {
            samplesHistory.add(targetIdx, count);
          } else {
            //samples history have been moved too far, it would have been incorrect to perform the requested registration of ticks
          }
        } finally {
          ticksAccumulateLock.unlockShared(ticksResetSharedLockStamp);
        }
      }
    }
  }

  private final void waitForCompletedWindowShiftSteps(final long samplesWindowShiftSteps) {
    if (!sequential) {
      assert completedSamplesWindowShiftStepsWaitStrategy != null;
      completedSamplesWindowShiftStepsWaitStrategy.await(() -> samplesWindowShiftSteps <= completedSamplesWindowShiftSteps);
    }
  }

  private final long count(final int fromInclusiveIdx, final int numberOfCellsToCount) {
    long result = 0;
    for (int i = 0, idx = fromInclusiveIdx; i < numberOfCellsToCount; i++, idx = nextSamplesHistoryIdx(idx)) {
      result += samplesHistory.get(idx);
    }
    return result;
  }

  /**
   * <pre>{@code
   *                                                |<-----------samplesWindowShiftSteps----------->|
   *                                                |                                               |
   *                            |<-samplesInterval->|                                      samplesHistoryIdx
   *                            |                   |                                               |                    idx
   * ...|8|9|0|1|2|3|4|5|6|7|8|9|0|1|2|3|4|5|6|7|8|9|0|1|2|3|4|5|6|7|8|9|0|1|2|3|4|5|6|7|8|9|0|1|2|3|4|5|6|7|8|9|0|1|2...
   *        (-------------------*-------------------]                                               |
   *        |                                       |       (-------------------*-------------------]
   *        |<--samplesHistory, historyLength==2--->|                                               |
   *        |      samplesHistory.length()==10      |                                               |                    t
   * -------|---------------------------------------|-----------------------------------------------|------------------->
   *                                            startNanos                               rightSamplesWindowBoundary
   * }</pre>
   */
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

  private final int samplesHistoryIdx(final long samplesWindowShiftSteps) {
    //the result can not be greater than samples.length, which is int, so it is a safe cast to int
    return (int)((samplesWindowShiftSteps + samplesHistory.length() - 1) % samplesHistory.length());
  }

  private final int nextSamplesHistoryIdx(final int idx) {
    return (idx + 1) % samplesHistory.length();
  }
}