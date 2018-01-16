package stinc.male.sandbox.ratmex.meter;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Function;
import java.util.function.LongConsumer;
import javax.annotation.Nullable;
import stinc.male.sandbox.ratmex.internal.util.ConversionsAndChecks;
import stinc.male.sandbox.ratmex.internal.util.Preconditions;
import static java.lang.Math.addExact;
import static java.lang.Math.min;
import static stinc.male.sandbox.ratmex.internal.util.Constants.EXCLUDE_ASSERTIONS_FROM_BYTECODE;
import static stinc.male.sandbox.ratmex.internal.util.Preconditions.checkNotNull;
import static stinc.male.sandbox.ratmex.internal.util.Utils.format;

/**
 * This is an almost complete implementation of {@link AbstractRateMeter}
 * which only requires a correct parameters to be provided to the
 * {@linkplain #AbstractRingBufferRateMeter(long, Duration, ConcurrentRateMeterConfig, Function, boolean) constructor}.
 * Depending on the arguments the constructed object can be either sequential, or concurrent.
 * <p>
 * This implementation of {@link RateMeter} uses a ring buffer with the underlying {@link LongArray}
 * to store and access a samples history.
 * <p>
 * <i>Advantages</i><br>
 * <ul>
 * <li>Unlike {@link AbstractNavigableMapRateMeter}, this implementation does not produces garbage,
 * unless customizable tools supplied to the constructor produce garbage
 * (e.g. {@link StampedLockStrategy} produces garbage because {@link StampedLock} does).</li>
 * <li>Unlike {@link AbstractNavigableMapRateMeter}, this implementation takes advantage of memory locality of data
 * stored in samples history.</li>
 * </ul>
 * <p>
 * <i>Disadvantages</i><br>
 * <ul>
 * <li>Unlike {@link AbstractNavigableMapRateMeter}, this implementation can not tolerate a large ratio of
 * {@link #getSamplesInterval()} to {@link #getTimeSensitivity()}.
 * The reason for this is that a ring buffer requires all objects representing samples to always exist,
 * and if the number of such objects (which is the same as the aforementioned ratio) is large,
 * then it can have a substantial negative effect on the performance.</li>
 * </ul>
 *
 * @param <C> A type of the {@linkplain #getConfig() configuration}.
 * @param <S> A type that represents {@linkplain #stats() statistics}.
 */
/*This class uses concurrent ring buffer with two cursors:
  a write cursor (atomicSamplesWindowShiftSteps) and a read cursor (completedSamplesWindowShiftSteps).
  The idea behind using two cursors is simple:
  - writes must be performed based on the write cursor;
  - externally visible reads must be performed based on the read cursor, but must then be validated based on the write cursor.
  Such an approach allows performing writes without blocking reads.*/
public abstract class AbstractRingBufferRateMeter<S, C extends ConcurrentRateMeterConfig>
    extends AbstractRateMeter<S, C> {
  private final boolean sequential;
  private final LongArray samplesHistory;//the length of this array is multiple of the historyLength
  private final long samplesWindowStepNanos;//essentially timeSensitivityNanos
  private final int cellsInSamplesWindow;
  private final int maxTicksCountAttempts;
  @Nullable
  private final LockStrategy ticksCountLock;//we don't need an analogous field for a sequential implementation
  @Nullable
  private final LockStrategy ticksAccumulateLock;//we don't need an analogous field for a sequential implementation
  @Nullable
  private final AtomicLong atomicSamplesWindowShiftSteps;//write cursor; samplesWindowShiftSteps for a concurrent implementation
  private long samplesWindowShiftSteps;//for a sequential implementation
  private volatile long completedSamplesWindowShiftSteps;//read cursor; we don't need an analogous field for a sequential implementation
  @Nullable
  private final WaitStrategy waitStrategy;//we don't need an analogous field for a sequential implementation

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
  protected AbstractRingBufferRateMeter(
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
    cellsInSamplesWindow = samplesHistory.length() / getConfig().getHistoryLength();
    assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || samplesWindowStepNanos == getTimeSensitivityNanos();
    assert EXCLUDE_ASSERTIONS_FROM_BYTECODE ||
        getSamplesIntervalNanos() * config.getHistoryLength() == samplesHistory.length() * samplesWindowStepNanos;
    maxTicksCountAttempts = getConfig().getMaxTicksCountAttempts() < 3 ? 3 : getConfig().getMaxTicksCountAttempts();
    this.sequential = sequential;
    if (sequential) {
      ticksCountLock = null;
      ticksAccumulateLock = null;
      atomicSamplesWindowShiftSteps = null;
      samplesWindowShiftSteps = 0;
      completedSamplesWindowShiftSteps = 0;
      waitStrategy = null;
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
      waitStrategy = config.getWaitStrategySupplier()
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
    if (sequential) {
      final long shiftSteps = samplesWindowShiftSteps;
      final long countFromShiftSteps = shiftSteps - cellsInSamplesWindow + 1;
      value = count(samplesHistoryIdx(countFromShiftSteps), cellsInSamplesWindow);
    } else {
      assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || atomicSamplesWindowShiftSteps != null;
      assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || ticksCountLock != null;
      long ticksCountSharedLockStamp = 0;
      try {
        final int samplesHistoryLength = samplesHistory.length();
        long completedShiftSteps = completedSamplesWindowShiftSteps;//must be read before atomicSamplesWindowShiftSteps
        final long shiftSteps = atomicSamplesWindowShiftSteps.get();
        assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || completedShiftSteps <= shiftSteps;
        int readIteration = 0;
        int trySharedLockAttempts = 1;
        while (true) {//if the number of tick threads is finite (which is true), then this loop successfully stops
          final long targetShiftSteps = completedShiftSteps;
          final long countFromShiftSteps = targetShiftSteps - cellsInSamplesWindow + 1;
          final long count = count(samplesHistoryIdx(countFromShiftSteps), cellsInSamplesWindow);
          final long newShiftSteps
              = atomicSamplesWindowShiftSteps.get();//atomicSamplesWindowShiftSteps, not completedSamplesWindowShiftSteps, it's important
          final long minShiftSteps = newShiftSteps - samplesHistoryLength + 1;
          if (minShiftSteps <= countFromShiftSteps) {//the samples history may has been moved while we were counting, but the count is still correct
            value = count;
            break;
          } else {//the samples history has been moved too far
            completedShiftSteps = completedSamplesWindowShiftSteps;//reread completedSamplesWindowShiftSteps
            /*We acquire the read lock to prevent concurrently running tick methods from moving the samples history too far.
              However since tick method acquires the write lock not always, but only when sees the read lock acquired,
              there is a race condition which still may lead to the samples history being moved,
              though the likelihood of such situation is now much less. Hence we still can exceed maxTicksCountAttempts,
              but eventually we are guaranteed to succeed in a final number of attempts*/
            if (ticksCountSharedLockStamp == 0 && readIteration >= maxTicksCountAttempts / 2) {
              //we have spent half of the read attempts, let us fall over to lock approach
              if (trySharedLockAttempts > 0) {
                trySharedLockAttempts--;
                ticksCountSharedLockStamp = ticksCountLock.trySharedLock();
              } else {
                ticksCountSharedLockStamp = ticksCountLock.sharedLock();
              }
            }
          }
          readIteration = addExact(readIteration, 1);
        }
      } finally {
        if (ticksCountSharedLockStamp != 0) {
          ticksCountLock.unlockShared(ticksCountSharedLockStamp);
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
    if (sequential) {
      final long shiftSteps = samplesWindowShiftSteps;
      final long countFromShiftSteps = shiftSteps - cellsInSamplesWindow + 1;
      reading.setTNanos(rightSamplesWindowBoundary(shiftSteps))
          .setValue(count(samplesHistoryIdx(countFromShiftSteps), cellsInSamplesWindow));
      readingDone = true;
    } else {
      assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || atomicSamplesWindowShiftSteps != null;
      assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || ticksCountLock != null;
      long ticksCountSharedLockStamp = 0;
      try {
        final int samplesHistoryLength = samplesHistory.length();
        long completedShiftSteps = completedSamplesWindowShiftSteps;//must be read before atomicSamplesWindowShiftSteps
        final long shiftSteps = atomicSamplesWindowShiftSteps.get();
        assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || completedShiftSteps <= shiftSteps;
        int readIteration = 0;
        int trySharedLockAttempts = 1;
        while (true) {//if the number of tick threads is finite (which is true), then this loop successfully stops
          final long targetShiftSteps = completedShiftSteps;
          final long countFromShiftSteps = targetShiftSteps - cellsInSamplesWindow + 1;
          final long count = count(samplesHistoryIdx(countFromShiftSteps), cellsInSamplesWindow);
          final long newShiftSteps
              = atomicSamplesWindowShiftSteps.get();//atomicSamplesWindowShiftSteps, not completedSamplesWindowShiftSteps, it's important
          final long minShiftSteps = newShiftSteps - samplesHistoryLength + 1;
          if (minShiftSteps <= countFromShiftSteps) {//the samples history may has been moved while we were counting, but the count is still correct
            reading.setTNanos(rightSamplesWindowBoundary(targetShiftSteps))
                .setValue(count);
            readingDone = true;
            break;
          } else {//the samples history has been moved too far
            completedShiftSteps = completedSamplesWindowShiftSteps;//reread completedSamplesWindowShiftSteps
            /*We acquire the read lock to prevent concurrently running tick methods from moving the samples history too far.
              However since tick method acquires the write lock not always, but only when sees the read lock acquired,
              there is a race condition which still may lead to the samples history being moved,
              though the likelihood of such situation is now much less. Hence we still can exceed maxTicksCountAttempts,
              but eventually we are guaranteed to succeed in a final number of attempts*/
            if (ticksCountSharedLockStamp == 0 && readIteration >= maxTicksCountAttempts / 2) {
              //we have spent half of the read attempts, let us fall over to lock approach
              if (trySharedLockAttempts > 0) {
                trySharedLockAttempts--;
                ticksCountSharedLockStamp = ticksCountLock.trySharedLock();
              } else {
                ticksCountSharedLockStamp = ticksCountLock.sharedLock();
              }
            }
          }
          readIteration = addExact(readIteration, 1);
        }
      } finally {
        if (ticksCountSharedLockStamp != 0) {
          ticksCountLock.unlockShared(ticksCountSharedLockStamp);
        }
      }
    }
    assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || readingDone;
    return reading;
  }

  //TODO compare single/multi thread for very short samples window; for various history lengths
  @Override
  public final void tick(final long count, final long tNanos) {
    checkArgument(tNanos, "tNanos");
    if (count != 0) {
      final int samplesHistoryLength = samplesHistory.length();
      final long targetShiftSteps = samplesWindowShiftSteps(tNanos);
      long shiftSteps;
      if (sequential) {
        shiftSteps = samplesWindowShiftSteps;
      } else {
        assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || atomicSamplesWindowShiftSteps != null;
        shiftSteps = atomicSamplesWindowShiftSteps.get();
      }
      if (shiftSteps - samplesHistoryLength < targetShiftSteps) {//tNanos is ahead of or within the samples history
        if (sequential) {
          if (shiftSteps < targetShiftSteps) {//we need to move the samples window
            final int targetIdx = samplesHistoryIdx(targetShiftSteps);
            samplesWindowShiftSteps = targetShiftSteps;
            final long numberOfSteps = targetShiftSteps - shiftSteps;//numberOfSteps is by how many steps we have moved the samples window
            if (numberOfSteps < samplesHistoryLength) {
              reset(shiftSteps + 1,
                      (int)numberOfSteps,//it's safe to cast to int
                      targetIdx, count, null);
            } else {
              resetAll(targetIdx, count);
            }
          } else {
            add(targetShiftSteps, count);
          }
        } else {
          assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || ticksCountLock != null;
          boolean moved = false;
          final long ticksCountExclusiveLockStamp = ticksCountLock.isSharedLocked() ? ticksCountLock.lock() : 0;
          while (shiftSteps < targetShiftSteps) {//try moving the samples window
            final long ticksAccumulateExclusiveLockStamp = ticksAccumulateLock == null ? 0 : ticksAccumulateLock.lock();
            try {
              //the only place where we change atomicSamplesWindowShiftSteps
              moved = atomicSamplesWindowShiftSteps.compareAndSet(shiftSteps, targetShiftSteps);
            } finally {
              if (ticksAccumulateExclusiveLockStamp != 0) {
                ticksAccumulateLock.unlock(ticksAccumulateExclusiveLockStamp);
              }
            }
            if (moved) {
              break;
            } else {//another thread has moved the samples window
              shiftSteps = atomicSamplesWindowShiftSteps.get();
            }
          }
          try {
            if (moved) {
              assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || shiftSteps < targetShiftSteps;
              final int targetIdx = samplesHistoryIdx(targetShiftSteps);
              final long numberOfSteps = targetShiftSteps - shiftSteps;//numberOfSteps is by how many steps we have moved the samples window
              final long waitForCompletedWindowShiftSteps = shiftSteps;
              waitStrategy.await(() -> waitForCompletedWindowShiftSteps <= completedSamplesWindowShiftSteps);//"serializing waiting condition"
              /*We are going to reset some (or all) samples because we need to reuse them (this is a ring buffer).
                Note that no other threads can concurrently reset samples because they are waiting on the "serializing waiting condition" above.*/
              if (numberOfSteps <= samplesHistoryLength) {//samples history is shifting step by step
                reset(shiftSteps + 1,
                        (int)numberOfSteps,//it's safe to cast to int
                        targetIdx, count,
                        (newCompletedShiftSteps) -> {
                          assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || newCompletedShiftSteps == completedSamplesWindowShiftSteps + 1;
                          //this and the below else block are the only places where we change completedSamplesWindowShiftSteps
                          completedSamplesWindowShiftSteps = newCompletedShiftSteps;//complete current step (actually just increment)
                        });
              } else {//shift samples history with a single leap
                resetAll(targetIdx, count);
                assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || shiftSteps == completedSamplesWindowShiftSteps;
                //this and the above if block are the only places where we change completedSamplesWindowShiftSteps
                completedSamplesWindowShiftSteps = targetShiftSteps;//complete all steps at once (leap)
              }
            } else {
              waitStrategy.await(() -> targetShiftSteps <= completedSamplesWindowShiftSteps);
              add(targetShiftSteps, count);
            }
          } finally {
            if (ticksCountExclusiveLockStamp != 0) {
              ticksCountLock.unlock(ticksCountExclusiveLockStamp);
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
    final long targetShiftSteps = samplesWindowShiftSteps(tNanos);
    final long completedShiftSteps = sequential ? samplesWindowShiftSteps : completedSamplesWindowShiftSteps;
    final long rightNanos = rightSamplesWindowBoundary(completedShiftSteps);
    final long measuredTNanos;
    if (targetShiftSteps <= completedShiftSteps) {//tNanos is within or behind the samples history
      measuredTNanos = rightSamplesWindowBoundary(completedShiftSteps);
    } else {//tNanos is ahead of the samples window
      measuredTNanos = tNanos;
    }
    return ConversionsAndChecks.rateAverage(measuredTNanos, getSamplesIntervalNanos(), getStartNanos(), ticksTotalCount());
  }

  /*The implementation of this method is an exact copy of rate(long, RateMeterReading) except for lines related to RateMeterReading.
    This is bad, but I don't see any other way to implement both methods in a garbage-free way.*/
  @Override
  public final double rate(final long tNanos) {
    checkArgument(tNanos, "tNanos");
    final double value;
    final long samplesIntervalNanos = getSamplesIntervalNanos();
    final int samplesHistoryLength = samplesHistory.length();
    final long completedShiftSteps;
    final long shiftSteps;
    if (sequential) {
      completedShiftSteps = samplesWindowShiftSteps;
      shiftSteps = completedShiftSteps;
    } else {
      assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || atomicSamplesWindowShiftSteps != null;
      completedShiftSteps = completedSamplesWindowShiftSteps;//must be read before atomicSamplesWindowShiftSteps
      shiftSteps = atomicSamplesWindowShiftSteps.get();
    }
    assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || completedShiftSteps <= shiftSteps;
    final long targetShiftSteps = samplesWindowShiftSteps(tNanos);
    final int cellsInSafeSamplesHistory = samplesHistoryLength - cellsInSamplesWindow;
    final long minSafeShiftSteps = shiftSteps - cellsInSafeSamplesHistory;//shiftSteps, not completedShiftSteps, it's important
    if (targetShiftSteps < minSafeShiftSteps) {//tNanos is behind the safe samples history, so return average over all samples
      final long measuredTNanos = rightSamplesWindowBoundary(completedShiftSteps);
      value = ConversionsAndChecks.rateAverage(//this is the same as rateAverage()
              measuredTNanos, samplesIntervalNanos, getStartNanos(), ticksTotalCount());
    } else {//tNanos is within or ahead of the safe samples history
      final long countFromShiftSteps = targetShiftSteps - cellsInSamplesWindow + 1;
      if (completedShiftSteps < countFromShiftSteps) {
        //tNanos is way too ahead of the completed part of the safe samples history and there are no samples for the requested tNanos
        value = 0;
      } else {
        assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || shiftSteps - samplesHistoryLength + 1 <= countFromShiftSteps;
        final long countToShiftSteps = min(completedShiftSteps, targetShiftSteps);
        assert EXCLUDE_ASSERTIONS_FROM_BYTECODE ||
                countFromShiftSteps <= countToShiftSteps && (countToShiftSteps - countFromShiftSteps + 1) <= cellsInSafeSamplesHistory;
        final int numberOfCellsToCount = (int)(countToShiftSteps - countFromShiftSteps) + 1;
        final long count = count(samplesHistoryIdx(countFromShiftSteps), numberOfCellsToCount);
        if (sequential) {
          value = count;
        } else {//check whether safe samples history has been moved too far while we were counting
          final long newShiftSteps
                  = atomicSamplesWindowShiftSteps.get();//atomicSamplesWindowShiftSteps, not completedSamplesWindowShiftSteps, it's important
          final long minShiftSteps = newShiftSteps - samplesHistoryLength + 1;
          if (countFromShiftSteps < minShiftSteps) {
            //the safe samples history has been moved too far, so return average over all samples
            final long measuredTNanos = rightSamplesWindowBoundary(completedSamplesWindowShiftSteps);
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
    final int samplesHistoryLength = samplesHistory.length();
    final long completedShiftSteps;
    final long shiftSteps;
    if (sequential) {
      completedShiftSteps = samplesWindowShiftSteps;
      shiftSteps = completedShiftSteps;
    } else {
      assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || atomicSamplesWindowShiftSteps != null;
      completedShiftSteps = completedSamplesWindowShiftSteps;//must be read before atomicSamplesWindowShiftSteps
      shiftSteps = atomicSamplesWindowShiftSteps.get();
    }
    assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || completedShiftSteps <= shiftSteps;
    final long targetShiftSteps = samplesWindowShiftSteps(tNanos);
    final int cellsInSafeSamplesHistory = samplesHistoryLength - cellsInSamplesWindow;
    final long minSafeShiftSteps = shiftSteps - cellsInSafeSamplesHistory;//shiftSteps, not completedShiftSteps, it's important
    if (targetShiftSteps < minSafeShiftSteps) {//tNanos is behind the safe samples history, so return average over all samples
      final long measuredTNanos = rightSamplesWindowBoundary(completedShiftSteps);
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
        assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || shiftSteps - samplesHistoryLength + 1 <= countFromShiftSteps;
        final long countToShiftSteps = min(completedShiftSteps, targetShiftSteps);
        assert EXCLUDE_ASSERTIONS_FROM_BYTECODE ||
            countFromShiftSteps <= countToShiftSteps && (countToShiftSteps - countFromShiftSteps + 1) <= cellsInSafeSamplesHistory;
        final int numberOfCellsToCount = (int)(countToShiftSteps - countFromShiftSteps) + 1;
        final long count = count(samplesHistoryIdx(countFromShiftSteps), numberOfCellsToCount);
        if (sequential) {
          reading.setValue(count);
          readingDone = true;
        } else {//check whether safe samples history has been moved too far while we were counting
          final long newShiftSteps
              = atomicSamplesWindowShiftSteps.get();//atomicSamplesWindowShiftSteps, not completedSamplesWindowShiftSteps, it's important
          final long minShiftSteps = newShiftSteps - samplesHistoryLength + 1;
          if (countFromShiftSteps < minShiftSteps) {
            //the safe samples history has been moved too far, so return average over all samples
            final long measuredTNanos = rightSamplesWindowBoundary(completedSamplesWindowShiftSteps);
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
    assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || readingDone;
    return reading;
  }

  /**
   * This method is called by {@link #tick(long, long)} not more than once per invocation of {@link #tick(long, long)}
   * when there is a chance that it might have failed to correctly register ticks.
   * Such a failure can only happen if this object is being used concurrently
   * (see {@link #AbstractRingBufferRateMeter(long, Duration, ConcurrentRateMeterConfig, Function, boolean)})
   * and {@link ConcurrentRateMeterConfig#isStrictTick()} is false.
   */
  protected void registerIncorrectlyRegisteredTicksEvent() {
  }

  private final void add(final long targetSamplesWindowShiftSteps, final long delta) {
    final int targetIdx = samplesHistoryIdx(targetSamplesWindowShiftSteps);
    final int samplesHistoryLength = samplesHistory.length();
    if (sequential) {
      samplesHistory.add(targetIdx, delta);
    } else {
      assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || atomicSamplesWindowShiftSteps != null;
      if (ticksAccumulateLock == null) {//not strict mode, no locking
        samplesHistory.add(targetIdx, delta);
        final long shiftSteps = atomicSamplesWindowShiftSteps.get();
        if (targetSamplesWindowShiftSteps <= shiftSteps - samplesHistoryLength) {
          //we could have registered (but it is not necessary) ticks at an incorrect instant because samples history have been moved too far
          registerIncorrectlyRegisteredTicksEvent();
        }
      } else {
        final long ticksResetSharedLockStamp = ticksAccumulateLock.sharedLock();
        try {
          final long shiftSteps = atomicSamplesWindowShiftSteps.get();
          if (shiftSteps - samplesHistoryLength < targetSamplesWindowShiftSteps) {
            samplesHistory.add(targetIdx, delta);
          } else {
            //samples history have been moved too far, it would have been incorrect to perform the requested registration of ticks
          }
        } finally {
          ticksAccumulateLock.unlockShared(ticksResetSharedLockStamp);
        }
      }
    }
  }

  private final void reset(
          final long fromInclusiveSamplesWindowShiftSteps,
          final int numberOfCellsToReset,
          final int targetIdx,
          final long count,
          @Nullable final LongConsumer postCellResetActionConsumingNewCompletedShiftSteps) {
    long newCompletedShiftSteps = fromInclusiveSamplesWindowShiftSteps;
    final int fromInclusiveIdx = samplesHistoryIdx(fromInclusiveSamplesWindowShiftSteps);
    final int samplesHistoryLength = samplesHistory.length();
    final int numberOfCellsOnTheRightIncludingFrom = samplesHistoryLength - fromInclusiveIdx;
    if (numberOfCellsToReset <= numberOfCellsOnTheRightIncludingFrom) {
      for (int idx = fromInclusiveIdx; idx < fromInclusiveIdx + numberOfCellsToReset; idx++) {
        samplesHistory.set(idx, idx == targetIdx ? count : 0);
        if (postCellResetActionConsumingNewCompletedShiftSteps != null) {
          postCellResetActionConsumingNewCompletedShiftSteps.accept(newCompletedShiftSteps);
          newCompletedShiftSteps++;
        }
      }
    } else {//numberOfCellsToReset > numberOfCellsOnTheRightIncludingFrom
      for (int idx = fromInclusiveIdx; idx < samplesHistoryLength; idx++) {
        samplesHistory.set(idx, idx == targetIdx ? count : 0);
        if (postCellResetActionConsumingNewCompletedShiftSteps != null) {
          postCellResetActionConsumingNewCompletedShiftSteps.accept(newCompletedShiftSteps);
          newCompletedShiftSteps++;
        }
      }
      final int numberOfNotResetCells = numberOfCellsToReset - numberOfCellsOnTheRightIncludingFrom;
      for (int idx = 0; idx < numberOfNotResetCells; idx++) {
        samplesHistory.set(idx, idx == targetIdx ? count : 0);
        if (postCellResetActionConsumingNewCompletedShiftSteps != null) {
          postCellResetActionConsumingNewCompletedShiftSteps.accept(newCompletedShiftSteps);
          newCompletedShiftSteps++;
        }
      }
    }
  }

  private final void resetAll(final int targetIdx, final long count) {
    final int samplesHistoryLength = samplesHistory.length();
    for (int idx = 0; idx < samplesHistoryLength; idx++) {
      samplesHistory.set(idx, idx == targetIdx ? count : 0);
    }
  }

  private final long count(final int fromInclusiveIdx, final int numberOfCellsToCount) {
    long result = 0;
    final int samplesHistoryLength = samplesHistory.length();
    final int numberOfCellsOnTheRightIncludingFrom = samplesHistoryLength - fromInclusiveIdx;
    if (numberOfCellsToCount <= numberOfCellsOnTheRightIncludingFrom) {
      for (int idx = fromInclusiveIdx; idx < fromInclusiveIdx + numberOfCellsToCount; idx++) {
        result += samplesHistory.get(idx);
      }
    } else {//numberOfCellsToCount > numberOfCellsOnTheRightIncludingFrom
      final int numberOfNotCountedCells = numberOfCellsToCount - numberOfCellsOnTheRightIncludingFrom;
      for (int idx = 0; idx < numberOfNotCountedCells; idx++) {
        result += samplesHistory.get(idx);
      }
      for (int idx = fromInclusiveIdx; idx < samplesHistoryLength; idx++) {
        result += samplesHistory.get(idx);
      }
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
    final int samplesHistoryLength = samplesHistory.length();
    //the result can not be greater than samples.length, which is int, so it is a safe cast to int
    return (int)((samplesWindowShiftSteps + samplesHistoryLength - 1) % samplesHistoryLength);
  }
}