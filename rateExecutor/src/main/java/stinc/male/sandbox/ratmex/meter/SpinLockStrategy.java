package stinc.male.sandbox.ratmex.meter;

import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.concurrent.ThreadSafe;
import static stinc.male.sandbox.ratmex.internal.util.Preconditions.checkArgument;
import static stinc.male.sandbox.ratmex.internal.util.Preconditions.checkNotNull;

/**
 * This implementation of {@link LockStrategy} does not distinguish shared and exclusive locking.
 * An {@link AtomicBoolean} is used as a state of this lock, and blocking is implemented via a {@link WaitStrategy}.
 */
@ThreadSafe
public final class SpinLockStrategy implements LockStrategy {
  private final AtomicBoolean atomicBoolean;
  private final WaitStrategy waitStrategy;

  /**
   * @param waitStrategy A {@link WaitStrategy} used for blocking. Must not be null.
   */
  public SpinLockStrategy(final WaitStrategy waitStrategy) {
    checkNotNull(waitStrategy, "waitStrategy");
    atomicBoolean = new AtomicBoolean();
    this.waitStrategy = waitStrategy;
  }

  /**
   * See {@link #tryLock()}.
   */
  @Override
  public final long trySharedLock() {
    return tryLock();
  }

  /**
   * See {@link #lock()}.
   */
  @Override
  public final long sharedLock() {
    return lock();
  }

  /**
   * See {@link #unlock(long)}.
   */
  @Override
  public final void unlockShared(final long stamp) {
    checkArgument(stamp != 0, "stamp", "Must not be 0");
    unlock(stamp);
  }

  /**
   * @return true if the lock is currently being held exclusively, false otherwise.
   */
  @Override
  public final boolean isSharedLocked() {
    return atomicBoolean.get();
  }

  /**
   * @return {@inheritDoc} If lock have been acquired, returns 1.
   */
  @Override
  public final long tryLock() {
    return atomicBoolean.compareAndSet(false, true) ? 1 : 0;
  }

  /**
   * @return {@inheritDoc} Returns 1.
   */
  @Override
  public final long lock() {
    waitStrategy.await(() -> atomicBoolean.compareAndSet(false, true));
    return 1;
  }

  @Override
  public final void unlock(final long stamp) {
    checkArgument(stamp != 0, "stamp", "Must not be 0");
    atomicBoolean.set(false);
  }
}