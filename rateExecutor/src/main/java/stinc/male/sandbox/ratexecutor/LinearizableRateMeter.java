package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import static stinc.male.sandbox.ratexecutor.Preconditions.checkNotNull;

@ThreadSafe
public final class LinearizableRateMeter implements RateMeter {
  private final SynchronizationType sync;
  private final RateMeter rm;
  @Nullable
  private final ReadWriteLock rwlock;
  @Nullable
  private final AtomicBoolean spinLock;

  public LinearizableRateMeter(final AbstractRateMeter rm, final SynchronizationType sync) {
    checkNotNull(rm, "rm");
    checkNotNull(sync, "sync");
    this.rm = rm;
    this.sync = sync;
    switch (sync) {
      case RW_LOCK: {
        rwlock = new ReentrantReadWriteLock();
        spinLock = null;
        break;
      }
      case SPIN_LOCK: {
        rwlock = null;
        spinLock = new AtomicBoolean();
        break;
      }
      default: {
        throw new IllegalArgumentException(String.format("The argument %s is illegal", "sync"));
      }
    }
  }

  @Override
  public final double rateAverage() {
    return rm.rateAverage();
  }

  @Override
  public final double rateAverage(final Duration unit) {
    return rm.rateAverage(unit);
  }

  @Override
  public final double rateAverage(final long tNanos, final Duration unit) {
    return rm.rateAverage(tNanos, unit);
  }

  @Override
  public final long rate() {
    return rm.rate();
  }

  @Override
  public final double rate(final Duration unit) {
    return rm.rate(unit);
  }

  @Override
  public final double rate(final long tNanos, final Duration unit) {
    return rm.rate(tNanos, unit);
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
  public final long ticksTotalCount() {
    readLock();
    try {
      return rm.ticksTotalCount();
    } finally {
      readUnlock();
    }
  }

  @Override
  public final RateMeterStats stats() {
    return rm.stats();//we know that RateMeterStats in AbstractRateMeter is thread-safe
  }

  @Override
  public final long rightSamplesWindowBoundary() {
    readLock();
    try {
      return rm.rightSamplesWindowBoundary();
    } finally {
      readUnlock();
    }
  }

  @Override
  public final long ticksCount() {
    readLock();
    try {
      return rm.ticksCount();
    } finally {
      readUnlock();
    }
  }

  @Override
  public final void tick(final long count, final long tNanos) {
    writeLock();
    try {
      rm.tick(count, tNanos);
    } finally {
      writeUnlock();
    }
  }

  @Override
  public final double rateAverage(final long tNanos) {
    readLock();
    try {
      return rm.rateAverage(tNanos);
    } finally {
      readUnlock();
    }
  }

  @Override
  public final double rate(final long tNanos) {
    readLock();
    try {
      return rm.rate(tNanos);
    } finally {
      readUnlock();
    }
  }

  private final void readLock() {
    if (sync == SynchronizationType.RW_LOCK) {
      rwlock.readLock().lock();
    } else {
      while (!spinLock.compareAndSet(false, true)) {
        Thread.yield();
      }
    }
  }

  private final void readUnlock() {
    if (sync == SynchronizationType.RW_LOCK) {
      rwlock.readLock().unlock();
    } else {
      spinLock.set(false);
    }
  }

  private final void writeLock() {
    if (sync == SynchronizationType.RW_LOCK) {
      rwlock.writeLock().lock();
    } else {
      while (!spinLock.compareAndSet(false, true)) {
        Thread.yield();
      }
    }
  }

  private final void writeUnlock() {
    if (sync == SynchronizationType.RW_LOCK) {
      rwlock.writeLock().unlock();
    } else {
      spinLock.set(false);
    }
  }

  public enum SynchronizationType {
    RW_LOCK,
    SPIN_LOCK
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName()
        + "(rm=" + rm
        + ", sync=" + sync
        + ')';
  }
}