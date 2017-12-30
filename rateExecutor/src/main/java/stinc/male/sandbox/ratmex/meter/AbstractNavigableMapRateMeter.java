package stinc.male.sandbox.ratmex.meter;

import java.time.Duration;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import stinc.male.sandbox.ratmex.util.internal.ConversionsAndChecks;
import stinc.male.sandbox.ratmex.util.internal.Preconditions;
import static stinc.male.sandbox.ratmex.util.internal.Preconditions.checkNotNull;

/**
 * @param <T>
 */
public abstract class AbstractNavigableMapRateMeter<C extends RateMeterConfig, T extends NavigableMap<Long, TicksCounter>> extends AbstractRateMeter<C> {
  private final boolean sequential;
  private final T samplesHistory;
  private final long timeSensitivityNanos;
  private final AtomicBoolean cleanInProgress;
  private volatile long cleanLastRightSamplesWindowBoundary;
  /**
   * (0; 1].
   * The bigger, the less frequently clean happens, but the older elements are maintained in the samples history.
   */
  private final double cleanRatio = 0.3;
  @Nullable
  private final StampedLock ticksCountRwLock;
  private final int maxTicksCountAttempts;

  /**
   * @param startNanos Starting point that is used to calculate elapsed nanoseconds.
   * @param samplesInterval Size of the samples window.
   * @param config Additional configuration parameters.
   * @param samplesHistorySupplier Specifies a supplier to use to create an object that will be
   *                         used to store samples history.
   *                         The {@link NavigableMap} provided by this supplier must use {@link NanosComparator}
   *                         as {@link NavigableMap#comparator() comparator}.
   * @param sequential
   */
  public AbstractNavigableMapRateMeter(
      final long startNanos,
      final Duration samplesInterval,
      final C config,
      final Supplier<T> samplesHistorySupplier,
      final boolean sequential) {
    super(startNanos, samplesInterval, config);
    checkNotNull(samplesHistorySupplier, "samplesHistorySupplier");
    samplesHistory = samplesHistorySupplier.get();
    Preconditions.checkArgument(samplesHistory.comparator() instanceof NanosComparator, "samplesSupplier",
        () -> "The comparator used by samples history map must be of type " + NanosComparator.class.getSimpleName());
    samplesHistory.put(startNanos, config.getTicksCounterSupplier().apply(0L));
    cleanInProgress = new AtomicBoolean();
    cleanLastRightSamplesWindowBoundary = getStartNanos();
    timeSensitivityNanos = config.getTimeSensitivity().toNanos();
    Preconditions.checkArgument(timeSensitivityNanos <= getSamplesIntervalNanos(), "config",
        () -> String.format("getTimeSensitivityNanos()=%s must be not greater than getSamplesIntervalNanos()=%s",
            timeSensitivityNanos, getSamplesIntervalNanos()));
    this.sequential = sequential;
    ticksCountRwLock = sequential ? null : new StampedLock();
    maxTicksCountAttempts = getConfig().getMaxTicksCountAttempts() < 3 ? 3 : getConfig().getMaxTicksCountAttempts();
  }

  @Override
  public long rightSamplesWindowBoundary() {
    return samplesHistory.lastKey();
  }

  /*The implementation of this method is an exact copy of ticksCount(RateMeterReading) except for lines related to RateMeterReading.
    This is bad, but I don't see any other way to implement both methods in a garbage-free way.*/
  @Override
  public long ticksCount() {
    long value = 0;
    boolean readingDone = false;
    final long samplesIntervalNanos = getSamplesIntervalNanos();
    long rightNanos = rightSamplesWindowBoundary();
    if (sequential) {
      final long leftNanos = rightNanos - samplesIntervalNanos;
      value = count(leftNanos, rightNanos);
      readingDone = true;
    } else {
      long ticksCountReadLockStamp = 0;
      try {
        for (int ri = 0; ri < maxTicksCountAttempts; ri++) {
          final long leftNanos = rightNanos - samplesIntervalNanos;
          value = count(leftNanos, rightNanos);
          readingDone = true;
          final long newRightNanos = rightSamplesWindowBoundary();
          if (NanosComparator.compare(newRightNanos - getConfig().getHl() * samplesIntervalNanos, leftNanos) <= 0) {//the samples window may has been moved while we were counting, but value is still correct
            break;
          } else {//the samples window has been moved too far
            rightNanos = newRightNanos;
            if (ticksCountReadLockStamp == 0 && ri >= maxTicksCountAttempts / 2 - 1) {//we have spent half of the read attempts, let us fall over to lock approach
              ticksCountReadLockStamp = ticksCountRwLock.readLock();
            }
          }
        }
      } finally {
        if (ticksCountReadLockStamp != 0) {
          ticksCountRwLock.unlockRead(ticksCountReadLockStamp);
        }
      }
    }
    assert readingDone;
    return value;
  }

  @Override
  public RateMeterReading ticksCount(final RateMeterReading reading) {
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
      long ticksCountReadLockStamp = 0;
      try {
        for (int ri = 0; ri < maxTicksCountAttempts; ri++) {
          final long leftNanos = rightNanos - samplesIntervalNanos;
          reading.setValue(count(leftNanos, rightNanos));
          readingDone = true;
          final long newRightNanos = rightSamplesWindowBoundary();
          if (NanosComparator.compare(newRightNanos - getConfig().getHl() * samplesIntervalNanos, leftNanos) <= 0) {//the samples window may has been moved while we were counting, but value is still correct
            break;
          } else {//the samples window has been moved too far
            rightNanos = newRightNanos;
            if (ticksCountReadLockStamp == 0 && ri >= maxTicksCountAttempts / 2 - 1) {//we have spent half of the read attempts, let us fall over to lock approach
              ticksCountReadLockStamp = ticksCountRwLock.readLock();
            }
          }
        }
      } finally {
        if (ticksCountReadLockStamp != 0) {
          ticksCountRwLock.unlockRead(ticksCountReadLockStamp);
        }
      }
    }
    assert readingDone;
    reading.setTNanos(rightNanos);
    return reading;
  }

  @Override
  public void tick(final long count, final long tNanos) {
    checkArgument(tNanos, "tNanos");
    if (count != 0) {
      final long rightNanos = rightSamplesWindowBoundary();
      final long leftHistoryNanos = rightNanos - getConfig().getHl() * getSamplesIntervalNanos();
      if (NanosComparator.compare(leftHistoryNanos, tNanos) < 0) {//tNanos is within the samples history
        @Nullable
        final TicksCounter existingSample;
        long ticksCountWriteLockStamp = (!sequential && ticksCountRwLock.isReadLocked()) ? ticksCountRwLock.writeLock() : 0;
        try {
          if (timeSensitivityNanos == 1) {
            final TicksCounter newSample = getConfig().getTicksCounterSupplier().apply(count);
            existingSample = samplesHistory.putIfAbsent(tNanos, newSample);
          } else {
            @Nullable
            final Entry<Long, TicksCounter> existingEntry = samplesHistory.floorEntry(tNanos);
            if (existingEntry != null && (tNanos - existingEntry.getKey()) <= timeSensitivityNanos) {
              existingSample = existingEntry.getValue();
            } else {
              final TicksCounter newSample = getConfig().getTicksCounterSupplier().apply(count);
              existingSample = samplesHistory.putIfAbsent(tNanos, newSample);
            }
          }
        } finally {
          if (ticksCountWriteLockStamp != 0) {
            ticksCountRwLock.unlockWrite(ticksCountWriteLockStamp);
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
  public double rateAverage(final long tNanos) {
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
  public double rate(final long tNanos) {
    checkArgument(tNanos, "tNanos");
    final double value;
    final long samplesIntervalNanos = getSamplesIntervalNanos();
    final long rightNanos = rightSamplesWindowBoundary();
    final long leftNanos = rightNanos - samplesIntervalNanos;
    if (NanosComparator.compare(tNanos, leftNanos) <= 0) {//tNanos is behind the samples window, so return average over all samples
      value = ConversionsAndChecks.rateAverage(rightNanos, samplesIntervalNanos, getStartNanos(), ticksTotalCount());//this is the same as rateAverage()
    } else {//tNanos is within or ahead of the samples window
      final long effectiveLeftNanos = tNanos - samplesIntervalNanos;
      if (NanosComparator.compare(rightNanos, effectiveLeftNanos) <= 0) {//tNanos is way too ahead of the samples window and there are no samples for the requested tNanos
        value = 0;
      } else {
        final long count = count(effectiveLeftNanos, tNanos);
        if (sequential) {
          value = count;
        } else {
          long newRightNanos = rightSamplesWindowBoundary();
          final long safeLeft = newRightNanos - getConfig().getHl() * samplesIntervalNanos;
          if (NanosComparator.compare(safeLeft, effectiveLeftNanos) <= 0) {//the samples window may has been moved while we were counting, but count is still correct
            value = count;
          } else {//the samples window has been moved too far, return average
            value = ConversionsAndChecks.rateAverage(newRightNanos, samplesIntervalNanos, getStartNanos(), ticksTotalCount());//this is the same as rateAverage()
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

  @Override
  public RateMeterReading rate(final long tNanos, final RateMeterReading reading) {
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
      final double value = ConversionsAndChecks.rateAverage(rightNanos, samplesIntervalNanos, getStartNanos(), ticksTotalCount());//this is the same as rateAverage()
      reading.setValue(value);
      readingDone = true;
    } else {//tNanos is within or ahead of the samples window
      final long effectiveLeftNanos = tNanos - samplesIntervalNanos;
      if (NanosComparator.compare(rightNanos, effectiveLeftNanos) <= 0) {//tNanos is way too ahead of the samples window and there are no samples for the requested tNanos
        reading.setValue(0);
        readingDone = true;
      } else {
        final long count = count(effectiveLeftNanos, tNanos);
        if (sequential) {
          reading.setValue(count);
          readingDone = true;
        } else {
          long newRightNanos = rightSamplesWindowBoundary();
          final long safeLeft = newRightNanos - getConfig().getHl() * samplesIntervalNanos;
          if (NanosComparator.compare(safeLeft, effectiveLeftNanos) <= 0) {//the samples window may has been moved while we were counting, but count is still correct
            reading.setValue(count);
            readingDone = true;
          } else {//the samples window has been moved too far, return average
            reading.setTNanos(newRightNanos);
            reading.setAccurate(false);
            final double value = ConversionsAndChecks.rateAverage(newRightNanos, samplesIntervalNanos, getStartNanos(), ticksTotalCount());//this is the same as rateAverage()
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
    final long samplesWindowShiftNanos = rightSamplesWindowBoundary - cleanLastRightSamplesWindowBoundary;
    final long samplesIntervalNanos = getSamplesIntervalNanos();
    //cleanRatio belongs to (0; 1], the bigger, the less frequently clean happens, but the older elements are maintained in the samples history
    final double cleanRatio = 0.3;
    final double maxRatio = getConfig().getHl() + cleanRatio;
    return maxRatio <= (double)samplesWindowShiftNanos / samplesIntervalNanos;
  }

  private final void clean(final long rightSamplesWindowBoundary) {
    if (cleanInProgress.compareAndSet(false, true)) {
      try {
        cleanLastRightSamplesWindowBoundary = rightSamplesWindowBoundary;
        final long leftNanos = rightSamplesWindowBoundary - getConfig().getHl() * getSamplesIntervalNanos();
        @Nullable
        final Long firstNanos = samplesHistory.firstKey();
        if (firstNanos != null && NanosComparator.compare(firstNanos.longValue(), leftNanos) < 0) {
          samplesHistory.subMap(firstNanos, true, leftNanos, false)//do not delete sample at leftNanos, because we still need it
              .clear();
        }
      } finally {
        cleanInProgress.set(false);
      }
    }
  }
}