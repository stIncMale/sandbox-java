package stinc.male.sandbox.ratmex.meter;

import java.util.concurrent.locks.StampedLock;
import javax.annotation.concurrent.ThreadSafe;
import static stinc.male.sandbox.ratmex.internal.util.Constants.EXCLUDE_ASSERTIONS_FROM_BYTECODE;
import static stinc.male.sandbox.ratmex.internal.util.Preconditions.checkArgument;

/**
 * This implementation of {@link LockStrategy} uses {@link StampedLock} and directly dispatches all methods to analogs in {@link StampedLock}.
 */
@ThreadSafe
public final class StampedLockStrategy implements LockStrategy {
  private final StampedLock stampedLock;

  public StampedLockStrategy() {
    stampedLock = new StampedLock();
  }

  @Override
  public final long trySharedLock() {
    return stampedLock.tryReadLock();
  }

  @Override
  public final long sharedLock() {
    final long result = stampedLock.readLock();
    assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || result != 0;
    return result;
  }

  @Override
  public final void unlockShared(final long stamp) {
    checkArgument(stamp != 0, "stamp", "Must not be 0");
    stampedLock.unlockRead(stamp);
  }

  @Override
  public final boolean isSharedLocked() {
    return stampedLock.isReadLocked();
  }

  @Override
  public final long tryLock() {
    return stampedLock.tryWriteLock();
  }

  @Override
  public final long lock() {
    final long result = stampedLock.writeLock();
    assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || result != 0;
    return result;
  }

  @Override
  public final void unlock(final long stamp) {
    checkArgument(stamp != 0, "stamp", "Must not be 0");
    stampedLock.unlockWrite(stamp);
  }
}