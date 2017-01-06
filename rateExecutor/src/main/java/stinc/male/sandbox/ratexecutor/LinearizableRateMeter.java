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
    lock();
    try {
      return rm.ticksTotalCount();
    } finally {
      unlock();
    }
  }

  @Override
  public final RateMeterStats stats() {
    return rm.stats();//we know that RateMeterStats in AbstractRateMeter is thread-safe
  }

  @Override
  public final long rightSamplesWindowBoundary() {
    lock();
    try {
      return rm.rightSamplesWindowBoundary();
    } finally {
      unlock();
    }
  }

  @Override
  public final long ticksCount() {
    lock();
    try {
      return rm.ticksCount();
    } finally {
      unlock();
    }
  }

  @Override
  public final void tick(final long count, final long tNanos) {
    lock();
    try {
      rm.tick(count, tNanos);
    } finally {
      unlock();
    }
  }

  @Override
  public final double rateAverage(final long tNanos) {
    lock();
    try {
      return rm.rateAverage(tNanos);
    } finally {
      unlock();
    }
  }

  @Override
  public final double rate(final long tNanos) {
    lock();
    try {
      return rm.rate(tNanos);
    } finally {
      unlock();
    }
  }

  private final void lock() {
    if (sync == SynchronizationType.RW_LOCK) {
      rwlock.readLock().lock();
    } else {
      while (!spinLock.compareAndSet(false, true)) {
        Thread.yield();
      }
    }
  }

  private final void unlock() {
    if (sync == SynchronizationType.RW_LOCK) {
      rwlock.readLock().lock();
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