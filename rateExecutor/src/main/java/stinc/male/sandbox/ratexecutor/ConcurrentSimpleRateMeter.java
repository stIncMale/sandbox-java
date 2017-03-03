package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import javax.annotation.concurrent.ThreadSafe;
import static stinc.male.sandbox.ratexecutor.Preconditions.checkNotNull;

@ThreadSafe
public final class ConcurrentSimpleRateMeter implements RateMeter {
  private final RateMeter rm;
  private final LockingStrategy lockingStrategy;

  public ConcurrentSimpleRateMeter(final RateMeter rm, final LockingStrategy lockingStrategy) {
    checkNotNull(rm, "rm");
    checkNotNull(lockingStrategy, "lockingStrategy");
    this.rm = rm;
    this.lockingStrategy = lockingStrategy;
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
  public final long rightSamplesWindowBoundary() {
    return rm.rightSamplesWindowBoundary();
  }

  @Override
  public final long ticksCount() {
    final long lockStamp = lockingStrategy.sharedLock();
    try {
      return rm.ticksCount();
    } finally {
      lockingStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final long ticksTotalCount() {
    final long lockStamp = lockingStrategy.sharedLock();
    try {
      return rm.ticksTotalCount();
    } finally {
      lockingStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final RateMeterReading ticksCount(final RateMeterReading reading) {
    final long lockStamp = lockingStrategy.sharedLock();
    try {
      return rm.ticksCount(reading);
    } finally {
      lockingStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final void tick(final long count, final long tNanos) {
    final long lockStamp = lockingStrategy.lock();
    try {
      rm.tick(count, tNanos);
    } finally {
      lockingStrategy.unlock(lockStamp);
    }
  }

  @Override
  public final double rateAverage() {
    final long lockStamp = lockingStrategy.sharedLock();
    try {
      return rm.rateAverage();
    } finally {
      lockingStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final double rateAverage(final Duration unit) {
    final long lockStamp = lockingStrategy.sharedLock();
    try {
      return rm.rateAverage(unit);
    } finally {
      lockingStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final double rateAverage(final long tNanos) {
    final long lockStamp = lockingStrategy.sharedLock();
    try {
      return rm.rateAverage(tNanos);
    } finally {
      lockingStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final double rateAverage(final long tNanos, final Duration unit) {
    final long lockStamp = lockingStrategy.sharedLock();
    try {
      return rm.rateAverage(tNanos, unit);
    } finally {
      lockingStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final long rate() {
    final long lockStamp = lockingStrategy.sharedLock();
    try {
      return rm.rate();
    } finally {
      lockingStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final RateMeterReading rate(final RateMeterReading reading) {
    final long lockStamp = lockingStrategy.sharedLock();
    try {
      return rm.rate(reading);
    } finally {
      lockingStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final double rate(final Duration unit) {
    final long lockStamp = lockingStrategy.sharedLock();
    try {
      return rm.rate(unit);
    } finally {
      lockingStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final RateMeterReading rate(final Duration unit, final RateMeterReading reading) {
    final long lockStamp = lockingStrategy.sharedLock();
    try {
      return rm.rate(unit, reading);
    } finally {
      lockingStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final double rate(final long tNanos) {
    final long lockStamp = lockingStrategy.sharedLock();
    try {
      return rm.rate(tNanos);
    } finally {
      lockingStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final RateMeterReading rate(final long tNanos, final RateMeterReading reading) {
    final long lockStamp = lockingStrategy.sharedLock();
    try {
      return rm.rate(tNanos, reading);
    } finally {
      lockingStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final double rate(final long tNanos, final Duration unit) {
    final long lockStamp = lockingStrategy.sharedLock();
    try {
      return rm.rate(tNanos, unit);
    } finally {
      lockingStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final RateMeterReading rate(final long tNanos, final Duration unit, final RateMeterReading reading) {
    final long lockStamp = lockingStrategy.sharedLock();
    try {
      return rm.rate(tNanos, unit, reading);
    } finally {
      lockingStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final RateMeterStats stats() {
    final long lockStamp = lockingStrategy.sharedLock();
    try {
      return rm.stats();
    } finally {
      lockingStrategy.unlockShared(lockStamp);
    }
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName()
        + "(rm=" + rm
        + ", lockingStrategy=" + lockingStrategy
        + ')';
  }
}