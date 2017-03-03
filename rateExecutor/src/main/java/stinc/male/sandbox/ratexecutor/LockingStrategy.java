package stinc.male.sandbox.ratexecutor;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public interface LockingStrategy {
  long sharedLock();

  void unlockShared(long stamp);

  long lock();

  void unlock(long stamp);
}