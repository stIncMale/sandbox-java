package stinc.male.sandbox.ratmex.meter;

import java.time.Duration;
import java.util.Optional;
import javax.annotation.concurrent.ThreadSafe;
import static stinc.male.sandbox.ratmex.util.internal.Preconditions.checkNotNull;

@ThreadSafe
public final class ConcurrentSimpleRateMeter implements RateMeter {
  private final RateMeter rm;
  private final LockStrategy lockStrategy;

  public ConcurrentSimpleRateMeter(final RateMeter rm, final LockStrategy lockStrategy) {
    checkNotNull(rm, "rm");
    checkNotNull(lockStrategy, "lockStrategy");
    this.rm = rm;
    this.lockStrategy = lockStrategy;
  }

  @Override
  public final long getStartNanos() {
    return rm.getStartNanos();
  }

  @Override
  public final Duration getSamplesInterval() {
    return rm.getSamplesInterval();
  }

  @Override
  public final Duration getTimeSensitivity() {
    return rm.getTimeSensitivity();
  }

  @Override
  public final long rightSamplesWindowBoundary() {
    return rm.rightSamplesWindowBoundary();
  }

  @Override
  public final long ticksCount() {
    final long lockStamp = lockStrategy.sharedLock();
    try {
      return rm.ticksCount();
    } finally {
      lockStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final long ticksTotalCount() {
    final long lockStamp = lockStrategy.sharedLock();
    try {
      return rm.ticksTotalCount();
    } finally {
      lockStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final RateMeterReading ticksCount(final RateMeterReading reading) {
    final long lockStamp = lockStrategy.sharedLock();
    try {
      return rm.ticksCount(reading);
    } finally {
      lockStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final void tick(final long count, final long tNanos) {
    final long lockStamp = lockStrategy.lock();
    try {
      rm.tick(count, tNanos);
    } finally {
      lockStrategy.unlock(lockStamp);
    }
  }

  @Override
  public final double rateAverage() {
    final long lockStamp = lockStrategy.sharedLock();
    try {
      return rm.rateAverage();
    } finally {
      lockStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final double rateAverage(final Duration unit) {
    final long lockStamp = lockStrategy.sharedLock();
    try {
      return rm.rateAverage(unit);
    } finally {
      lockStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final double rateAverage(final long tNanos) {
    final long lockStamp = lockStrategy.sharedLock();
    try {
      return rm.rateAverage(tNanos);
    } finally {
      lockStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final double rateAverage(final long tNanos, final Duration unit) {
    final long lockStamp = lockStrategy.sharedLock();
    try {
      return rm.rateAverage(tNanos, unit);
    } finally {
      lockStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final long rate() {
    final long lockStamp = lockStrategy.sharedLock();
    try {
      return rm.rate();
    } finally {
      lockStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final RateMeterReading rate(final RateMeterReading reading) {
    final long lockStamp = lockStrategy.sharedLock();
    try {
      return rm.rate(reading);
    } finally {
      lockStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final double rate(final Duration unit) {
    final long lockStamp = lockStrategy.sharedLock();
    try {
      return rm.rate(unit);
    } finally {
      lockStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final RateMeterReading rate(final Duration unit, final RateMeterReading reading) {
    final long lockStamp = lockStrategy.sharedLock();
    try {
      return rm.rate(unit, reading);
    } finally {
      lockStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final double rate(final long tNanos) {
    final long lockStamp = lockStrategy.sharedLock();
    try {
      return rm.rate(tNanos);
    } finally {
      lockStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final RateMeterReading rate(final long tNanos, final RateMeterReading reading) {
    final long lockStamp = lockStrategy.sharedLock();
    try {
      return rm.rate(tNanos, reading);
    } finally {
      lockStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final double rate(final long tNanos, final Duration unit) {
    final long lockStamp = lockStrategy.sharedLock();
    try {
      return rm.rate(tNanos, unit);
    } finally {
      lockStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final RateMeterReading rate(final long tNanos, final Duration unit, final RateMeterReading reading) {
    final long lockStamp = lockStrategy.sharedLock();
    try {
      return rm.rate(tNanos, unit, reading);
    } finally {
      lockStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final Optional<? extends RateMeterStats> stats() {
    final long lockStamp = lockStrategy.sharedLock();
    try {
      return rm.stats();
    } finally {
      lockStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName()
        + "{rm=" + rm
        + ", lockStrategy=" + lockStrategy
        + '}';
  }
}