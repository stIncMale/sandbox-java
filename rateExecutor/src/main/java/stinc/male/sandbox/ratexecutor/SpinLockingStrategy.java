package stinc.male.sandbox.ratexecutor;

import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public final class SpinLockingStrategy implements LockingStrategy {
  private final AtomicBoolean atomicBoolean;
  private final WaitStrategy waitStrategy;

  public SpinLockingStrategy(final WaitStrategy waitStrategy) {
    atomicBoolean = new AtomicBoolean();
    this.waitStrategy = waitStrategy;
  }

  @Override
  public final long sharedLock() {
    return lock();
  }

  @Override
  public final void unlockShared(final long stamp) {
    unlock(stamp);
  }

  @Override
  public final long lock() {
    waitStrategy.await(() -> atomicBoolean.compareAndSet(false, true));
    return 0;
  }

  @Override
  public final void unlock(final long stamp) {
    atomicBoolean.set(false);
  }
}