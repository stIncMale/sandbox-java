package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public final class LinearizableRateMeter implements RateMeter {
  private final RateMeter rm;
  private final ReadWriteLock rwlock;

  public LinearizableRateMeter(final AbstractRateMeter rm) {
    Preconditions.checkNotNull(rm, "rm");
    this.rm = rm;
    rwlock = new ReentrantReadWriteLock();
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
    rwlock.readLock().lock();
    try {
      return rm.ticksTotalCount();
    } finally {
      rwlock.readLock().unlock();
    }
  }

  @Override
  public final RateMeterStats stats() {
    return rm.stats();//we know that RateMeterStats in AbstractRateMeter is thread-safe
  }

  @Override
  public final long rightSamplesWindowBoundary() {
    rwlock.readLock().lock();
    try {
      return rm.rightSamplesWindowBoundary();
    } finally {
      rwlock.readLock().unlock();
    }
  }

  @Override
  public final long ticksCount() {
    rwlock.readLock().lock();
    try {
      return rm.ticksCount();
    } finally {
      rwlock.readLock().unlock();
    }
  }

  @Override
  public final void tick(final long count, final long tNanos) {
    rwlock.readLock().lock();
    try {
      rm.tick(count, tNanos);
    } finally {
      rwlock.readLock().unlock();
    }
  }

  @Override
  public final double rateAverage(final long tNanos) {
    rwlock.readLock().lock();
    try {
      return rm.rateAverage(tNanos);
    } finally {
      rwlock.readLock().unlock();
    }
  }

  @Override
  public final double rate(final long tNanos) {
    rwlock.readLock().lock();
    try {
      return rm.rate(tNanos);
    } finally {
      rwlock.readLock().unlock();
    }
  }
}