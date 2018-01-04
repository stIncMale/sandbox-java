package stinc.male.sandbox.ratmex.meter;

import java.util.concurrent.locks.StampedLock;
import javax.annotation.concurrent.ThreadSafe;
import static stinc.male.sandbox.ratmex.internal.util.Preconditions.checkArgument;

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
    assert result != 0;
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
  public final long lock() {
    final long result = stampedLock.writeLock();
    assert result != 0;
    return result;
  }

  @Override
  public final void unlock(final long stamp) {
    checkArgument(stamp != 0, "stamp", "Must not be 0");
    stampedLock.unlockWrite(stamp);
  }
}