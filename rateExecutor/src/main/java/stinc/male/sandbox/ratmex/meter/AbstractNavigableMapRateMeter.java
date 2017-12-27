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
  private final AtomicBoolean gcInProgress;
  private volatile long gcLastRightSamplesWindowBoundary;
  @Nullable
  private final StampedLock ticksCountRwLock;
  private final double gcRatio = 0.3;//(0; 1] the bigger, the less frequently GC happens, but the older elements are maintained in the samples history.
  private final long maxTicksCountAttempts;

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
    gcInProgress = new AtomicBoolean();
    gcLastRightSamplesWindowBoundary = getStartNanos();
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

  @Override
  public long ticksCount() {
    long result = 0;
    final long samplesIntervalNanos = getSamplesIntervalNanos();
    long rightNanos = rightSamplesWindowBoundary();
    if (sequential) {
      final long leftNanos = rightNanos - samplesIntervalNanos;
      result = count(leftNanos, rightNanos);
    } else {
      long ticksCountReadLockStamp = 0;
      try {
        for (long ri = 0; ri < maxTicksCountAttempts; ri++) {
          final long leftNanos = rightNanos - samplesIntervalNanos;
          result = count(leftNanos, rightNanos);
          final long newRightNanos = rightSamplesWindowBoundary();
          if (NanosComparator.compare(newRightNanos - getConfig().getHl() * samplesIntervalNanos, leftNanos) <= 0) {//the samples window may has been moved while we were counting, but result is still correct
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
    return result;
  }

  @Override
  public RateMeterReading ticksCount(final RateMeterReading reading) {
    checkNotNull(reading, "reading");
    reading.setAccurate(true);
    long value = 0;
    final long samplesIntervalNanos = getSamplesIntervalNanos();
    long rightNanos = rightSamplesWindowBoundary();
    if (sequential) {
      final long leftNanos = rightNanos - samplesIntervalNanos;
      value = count(leftNanos, rightNanos);
    } else {
      long ticksCountReadLockStamp = 0;
      try {
        for (int ri = 0; ri < maxTicksCountAttempts; ri++) {
          final long leftNanos = rightNanos - samplesIntervalNanos;
          value = count(leftNanos, rightNanos);
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
    reading.setTNanos(rightNanos).setValue(value);
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
      if (gcRequired(rightNanos)) {
        gc(rightNanos);
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

  @Override
  public double rate(final long tNanos) {
    checkArgument(tNanos, "tNanos");
    final double result;
    final long samplesIntervalNanos = getSamplesIntervalNanos();
    final long rightNanos = rightSamplesWindowBoundary();
    final long leftNanos = rightNanos - samplesIntervalNanos;
    if (NanosComparator.compare(tNanos, leftNanos) <= 0) {//tNanos is behind the samples window, so return average over all samples
      result = ConversionsAndChecks.rateAverage(rightNanos, samplesIntervalNanos, getStartNanos(), ticksTotalCount());//this is the same as rateAverage()
    } else {//tNanos is within or ahead of the samples window
      final long effectiveLeftNanos = tNanos - samplesIntervalNanos;
      if (NanosComparator.compare(rightNanos, effectiveLeftNanos) <= 0) {//tNanos is way too ahead of the samples window and there are no samples for the requested tNanos
        result = 0;
      } else {
        final long count = count(effectiveLeftNanos, tNanos);
        if (sequential) {
          result = count;
        } else {
          long newRightNanos = rightSamplesWindowBoundary();
          final long safeLeft = newRightNanos - getConfig().getHl() * samplesIntervalNanos;
          if (NanosComparator.compare(safeLeft, effectiveLeftNanos) <= 0) {//the samples window may has been moved while we were counting, but count is still correct
            result = count;
          } else {//the samples window has been moved too far, return average
            getStats().ifPresent(ConcurrentRateMeterStats::accountFailedAccuracyEventForRate);
            result = ConversionsAndChecks.rateAverage(newRightNanos, samplesIntervalNanos, getStartNanos(), ticksTotalCount());//this is the same as rateAverage()
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
    final double value;
    final long samplesIntervalNanos = getSamplesIntervalNanos();
    final long rightNanos = rightSamplesWindowBoundary();
    final long leftNanos = rightNanos - samplesIntervalNanos;
    if (NanosComparator.compare(tNanos, leftNanos) <= 0) {//tNanos is behind the samples window, so return average over all samples
      value = ConversionsAndChecks.rateAverage(rightNanos, samplesIntervalNanos, getStartNanos(), ticksTotalCount());//this is the same as rateAverage()
      reading.setTNanos(rightNanos);
      reading.setAccurate(false);
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
            reading.setTNanos(newRightNanos);
            reading.setAccurate(false);
            getStats().ifPresent(ConcurrentRateMeterStats::accountFailedAccuracyEventForRate);
            value = ConversionsAndChecks.rateAverage(newRightNanos, samplesIntervalNanos, getStartNanos(), ticksTotalCount());//this is the same as rateAverage()
          }
        }
      }
    }
    reading.setValue(value);
    return reading;
  }

  private final long count(final long fromExclusiveNanos, final long toInclusiveNanos) {
    return samplesHistory.subMap(fromExclusiveNanos, false, toInclusiveNanos, true)
        .values()
        .stream()
        .mapToLong(TicksCounter::get)
        .sum();
  }

  private final boolean gcRequired(final long rightSamplesWindowBoundary) {
    final long samplesWindowShiftNanos = rightSamplesWindowBoundary - gcLastRightSamplesWindowBoundary;
    final long samplesIntervalNanos = getSamplesIntervalNanos();
    final double maxRatio = getConfig().getHl() + gcRatio;
    final boolean result = maxRatio <= (double)samplesWindowShiftNanos / samplesIntervalNanos;
    return result;
  }

  private final void gc(final long rightSamplesWindowBoundary) {
    if (gcInProgress.compareAndSet(false, true)) {
      try {
        gcLastRightSamplesWindowBoundary = rightSamplesWindowBoundary;
        final long leftNanos = rightSamplesWindowBoundary - getConfig().getHl() * getSamplesIntervalNanos();
        @Nullable
        final Long firstNanos = samplesHistory.firstKey();
        if (firstNanos != null && NanosComparator.compare(firstNanos.longValue(), leftNanos) < 0) {
          samplesHistory.subMap(firstNanos, true, leftNanos, false)//do not delete sample at leftNanos, because we still need it
              .clear();
        }
      } finally {
        gcInProgress.set(false);
      }
    }
  }
}