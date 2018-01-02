package stinc.male.sandbox.ratmex.meter;

import java.time.Duration;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import stinc.male.sandbox.ratmex.internal.util.ConversionsAndChecks;
import stinc.male.sandbox.ratmex.internal.util.Preconditions;
import static stinc.male.sandbox.ratmex.internal.util.Preconditions.checkNotNull;

/**
 * This implementation uses {@link NavigableMap} to store and access a samples history.
 * There is {@linkplain ConcurrentSkipListMap at least one} concurrent implementation of this interface,
 * and it allows a fairly simple implementation of {@link RateMeter}.
 * <p>
 * <i>Advantages</i><br>
 * Unlike {@link AbstractRingBufferRateMeter}, this implementation tolerates a large ratio of
 * {@link RateMeter#getSamplesInterval()} and {@link RateMeter#getTimeSensitivity()}.
 * The reason for this is that it only creates objects representing samples when it is necessary,
 * hence potentially reducing the number of samples that must be added up to count the {@linkplain #ticksCount() current ticks}.
 * <p>
 * <i>Disadvantages</i><br>
 * Unlike {@link AbstractRingBufferRateMeter}, this implementation produces garbage.
 *
 * @param <C> A type of the {@linkplain #getConfig() configuration}.
 */
public abstract class AbstractNavigableMapRateMeter<C extends RateMeterConfig> extends AbstractRateMeter<C> {
  private final boolean sequential;
  private final NavigableMap<Long, TicksCounter> samplesHistory;
  private final long timeSensitivityNanos;
  private final int maxTicksCountAttempts;
  @Nullable
  private final AtomicBoolean atomicCleanInProgress;//we don't need an analogous field for a sequential implementation
  private volatile long volatileCleanLastRightSamplesWindowBoundary;//cleanLastRightSamplesWindowBoundary for a concurrent implementation
  private long cleanLastRightSamplesWindowBoundary;//for a sequential implementation
  @Nullable
  private final StampedLock ticksCountLock;//TODO use LockStrategy//we don't need an analogous field for a sequential implementation

  /**
   * @param startNanos A {@linkplain #getStartNanos() starting point} that is used to calculate elapsed time in nanoseconds (tNanos).
   * @param samplesInterval A size of the {@linkplain #getSamplesInterval() samples window}.
   * Must not be null, see {@link RateMeter} for valid values.
   * @param config An additional {@linkplain #getConfig() configuration}. Must not be null.
   * @param samplesHistorySupplier A supplier providing an object that will be used to store and access the samples history.
   * The {@link NavigableMap} provided by this supplier must use {@link NanosComparator} as {@link NavigableMap#comparator() comparator},
   * and must be thread-safe if {@code sequential} argument is false.
   * @param sequential Specifies whether the {@link RateMeter} must be thread-safe (will be used concurrently, so the value is false),
   * or not (will be used sequentially, so the value is true).
   */
  public AbstractNavigableMapRateMeter(
      final long startNanos,
      final Duration samplesInterval,
      final C config,
      final Supplier<? extends NavigableMap<Long, TicksCounter>> samplesHistorySupplier,
      final boolean sequential) {
    super(startNanos, samplesInterval, config);
    checkNotNull(samplesHistorySupplier, "samplesHistorySupplier");
    samplesHistory = samplesHistorySupplier.get();
    Preconditions.checkArgument(samplesHistory.comparator() instanceof NanosComparator, "samplesSupplier",
        () -> "The comparator used by samples history map must be of type " + NanosComparator.class.getSimpleName());
    samplesHistory.put(
        startNanos,
        config.getTicksCounterSupplier()
            .apply(0L));
    atomicCleanInProgress = sequential ? null : new AtomicBoolean();
    volatileCleanLastRightSamplesWindowBoundary = getStartNanos();
    cleanLastRightSamplesWindowBoundary = getStartNanos();
    timeSensitivityNanos = config.getTimeSensitivity()
        .toNanos();
    Preconditions.checkArgument(
        timeSensitivityNanos <= getSamplesIntervalNanos(), "config",
        () -> String.format("getTimeSensitivityNanos()=%s must be not greater than getSamplesIntervalNanos()=%s",
            timeSensitivityNanos, getSamplesIntervalNanos()));
    ticksCountLock = sequential ? null : new StampedLock();
    this.sequential = sequential;
    maxTicksCountAttempts = getConfig().getMaxTicksCountAttempts() < 3 ? 3 : getConfig().getMaxTicksCountAttempts();
  }

  @Override
  public final long rightSamplesWindowBoundary() {
    return samplesHistory.lastKey();
  }

  /*The implementation of this method is an exact copy of ticksCount(RateMeterReading) except for lines related to RateMeterReading.
    This is bad, but I don't see any other way to implement both methods in a garbage-free way.*/
  @Override
  public final long ticksCount() {
    long value = 0;
    boolean readingDone = false;
    final long samplesIntervalNanos = getSamplesIntervalNanos();
    long rightNanos = rightSamplesWindowBoundary();
    if (sequential) {
      final long leftNanos = rightNanos - samplesIntervalNanos;
      value = count(leftNanos, rightNanos);
      readingDone = true;
    } else {
      assert ticksCountLock != null;
      long ticksCountReadLockStamp = 0;
      try {
        for (int ri = 0; ri < Integer.MAX_VALUE; ri++) {//if the number of tick threads is finite (should be true), then this loop successfully stops
          final long leftNanos = rightNanos - samplesIntervalNanos;
          value = count(leftNanos, rightNanos);
          readingDone = true;
          final long newRightNanos = rightSamplesWindowBoundary();
          if (NanosComparator.compare(newRightNanos - getConfig().getHistoryLength() * samplesIntervalNanos, leftNanos) <= 0) {
            //the samples window may has been moved while we were counting, but value is still correct
            break;
          } else {//the samples window has been moved too far
            rightNanos = newRightNanos;
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
    final long samplesIntervalNanos = getSamplesIntervalNanos();
    long rightNanos = rightSamplesWindowBoundary();
    if (sequential) {
      final long leftNanos = rightNanos - samplesIntervalNanos;
      reading.setValue(count(leftNanos, rightNanos));
      readingDone = true;
    } else {
      assert ticksCountLock != null;
      long ticksCountReadLockStamp = 0;
      try {
        for (int ri = 0; ri < Integer.MAX_VALUE; ri++) {//if the number of tick threads is finite (should be true), then this loop successfully stops
          final long leftNanos = rightNanos - samplesIntervalNanos;
          reading.setValue(count(leftNanos, rightNanos));
          readingDone = true;
          final long newRightNanos = rightSamplesWindowBoundary();
          if (NanosComparator.compare(newRightNanos - getConfig().getHistoryLength() * samplesIntervalNanos, leftNanos) <= 0) {
            //the samples window may has been moved while we were counting, but value is still correct
            break;
          } else {//the samples window has been moved too far
            rightNanos = newRightNanos;
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
    reading.setTNanos(rightNanos);
    return reading;
  }

  @Override
  public final void tick(final long count, final long tNanos) {
    checkArgument(tNanos, "tNanos");
    if (count != 0) {
      final long rightNanos = rightSamplesWindowBoundary();
      final long leftHistoryNanos = rightNanos - getConfig().getHistoryLength() * getSamplesIntervalNanos();
      if (NanosComparator.compare(leftHistoryNanos, tNanos) < 0) {//tNanos is ahead of or within the samples history
        @Nullable
        final TicksCounter existingSample;
        final long ticksCountWriteLockStamp;
        if (sequential) {
          ticksCountWriteLockStamp = 0;
        } else {
          assert ticksCountLock != null;
          /*We acquire the write lock only when see the read lock acquired by tickCount method,
            which acquires the read lock to prevent concurrently running tick methods from moving the samples window too far.
            There is a race condition which still may lead to the samples window being moved,
            though the likelihood of such situation is now much less, and tickCount method expresses failed reads via RateMeterReading*/
          ticksCountWriteLockStamp = ticksCountLock.isReadLocked() ? ticksCountLock.writeLock() : 0;
        }
        try {
          if (timeSensitivityNanos == 1) {
            final TicksCounter newSample = getConfig().getTicksCounterSupplier()
                .apply(count);
            existingSample = samplesHistory.putIfAbsent(tNanos, newSample);
          } else {
            @Nullable
            final Entry<Long, TicksCounter> existingEntry = samplesHistory.floorEntry(tNanos);
            if (existingEntry != null && (tNanos - existingEntry.getKey()) <= timeSensitivityNanos) {
              assert tNanos - existingEntry.getKey() >= 0;
              existingSample = existingEntry.getValue();
            } else {
              final TicksCounter newSample = getConfig().getTicksCounterSupplier()
                  .apply(count);
              existingSample = samplesHistory.putIfAbsent(tNanos, newSample);
            }
          }
        } finally {
          if (ticksCountWriteLockStamp != 0) {
            ticksCountLock.unlockWrite(ticksCountWriteLockStamp);
          }
        }
        if (existingSample != null) {//we need to merge samples
          existingSample.add(count);
        }
      }
      getTicksTotalCounter().add(count);
      if (cleanRequired(rightNanos)) {
        clean(rightNanos);
      }
    }
  }

  @Override
  public final double rateAverage(final long tNanos) {
    checkArgument(tNanos, "tNanos");
    long rightNanos = rightSamplesWindowBoundary();
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
    final long rightNanos = rightSamplesWindowBoundary();
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
        final long count = count(effectiveLeftNanos, tNanos);
        if (sequential) {
          value = count;
        } else {
          long newRightNanos = rightSamplesWindowBoundary();
          final long safeLeft = newRightNanos - getConfig().getHistoryLength() * samplesIntervalNanos;
          if (NanosComparator.compare(safeLeft, effectiveLeftNanos) <= 0) {
            //the samples window may has been moved while we were counting, but count is still correct
            value = count;
          } else {//the samples window has been moved too far, return average
            value = ConversionsAndChecks.rateAverage(//this is the same as rateAverage()
                newRightNanos,
                samplesIntervalNanos,
                getStartNanos(),
                ticksTotalCount());
            @Nullable
            final ConcurrentRateMeterStats stats = getStats();
            if (stats != null) {
              stats.accountFailedAccuracyEventForRate();
            }
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
    final long rightNanos = rightSamplesWindowBoundary();
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
        final long count = count(effectiveLeftNanos, tNanos);
        if (sequential) {
          reading.setValue(count);
          readingDone = true;
        } else {
          long newRightNanos = rightSamplesWindowBoundary();
          final long safeLeft = newRightNanos - getConfig().getHistoryLength() * samplesIntervalNanos;
          if (NanosComparator.compare(safeLeft, effectiveLeftNanos) <= 0) {
            //the samples window may has been moved while we were counting, but count is still correct
            reading.setValue(count);
            readingDone = true;
          } else {//the samples window has been moved too far, return average
            reading.setTNanos(newRightNanos);
            reading.setAccurate(false);
            final double value = ConversionsAndChecks.rateAverage(//this is the same as rateAverage()
                newRightNanos,
                samplesIntervalNanos,
                getStartNanos(),
                ticksTotalCount());
            reading.setValue(value);
            readingDone = true;
            @Nullable
            final ConcurrentRateMeterStats stats = getStats();
            if (stats != null) {
              stats.accountFailedAccuracyEventForRate();
            }
          }
        }
      }
    }
    assert readingDone;
    return reading;
  }

  private final long count(final long fromExclusiveNanos, final long toInclusiveNanos) {
    return samplesHistory.subMap(fromExclusiveNanos, false, toInclusiveNanos, true)
        .values()
        .stream()
        .mapToLong(TicksCounter::get)
        .sum();
  }

  private final boolean cleanRequired(final long rightSamplesWindowBoundary) {
    final long samplesWindowShiftNanos;
    if (sequential) {
      samplesWindowShiftNanos = rightSamplesWindowBoundary - cleanLastRightSamplesWindowBoundary;
    } else {
      samplesWindowShiftNanos = rightSamplesWindowBoundary - volatileCleanLastRightSamplesWindowBoundary;
    }
    final long samplesIntervalNanos = getSamplesIntervalNanos();
    //cleanRatio belongs to (0; 1], the bigger, the less frequently clean happens, but the older elements are maintained in the samples history
    final double cleanRatio = 0.3;
    final double maxRatio = getConfig().getHistoryLength() + cleanRatio;
    return maxRatio <= (double)samplesWindowShiftNanos / samplesIntervalNanos;
  }

  private final void clean(final long rightSamplesWindowBoundary) {
    assert sequential || atomicCleanInProgress != null;
    if (sequential || atomicCleanInProgress.compareAndSet(false, true)) {
      try {
        if (sequential) {
          cleanLastRightSamplesWindowBoundary = rightSamplesWindowBoundary;
        } else {
          volatileCleanLastRightSamplesWindowBoundary = rightSamplesWindowBoundary;
        }
        final long leftNanos = rightSamplesWindowBoundary - getConfig().getHistoryLength() * getSamplesIntervalNanos();
        @Nullable
        final Long firstNanos = samplesHistory.firstKey();
        if (firstNanos != null && NanosComparator.compare(firstNanos.longValue(), leftNanos) < 0) {
          samplesHistory.subMap(firstNanos, true, leftNanos, false)//do not delete sample at leftNanos, because we still need it
              .clear();
        }
      } finally {
        if (!sequential) {
          atomicCleanInProgress.set(false);
        }
      }
    }
  }
}