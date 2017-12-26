package stinc.male.sandbox.ratmex.meter;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public interface LockStrategy {
  long sharedLock();

  void unlockShared(long stamp);

  long lock();

  void unlock(long stamp);
}