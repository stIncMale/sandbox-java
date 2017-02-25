package stinc.male.sandbox.ratexecutor;

import java.util.concurrent.locks.StampedLock;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public final class StampedLockStrategy implements LockStrategy {
  private final StampedLock stampedLock;

  public StampedLockStrategy() {
    stampedLock = new StampedLock();
  }

  @Override
  public final long sharedLock() {
    return stampedLock.readLock();
  }

  @Override
  public final void unlockShared(final long stamp) {
    stampedLock.unlockRead(stamp);
  }

  @Override
  public final long lock() {
    return stampedLock.writeLock();
  }

  @Override
  public final void unlock(final long stamp) {
    stampedLock.unlockWrite(stamp);
  }
}