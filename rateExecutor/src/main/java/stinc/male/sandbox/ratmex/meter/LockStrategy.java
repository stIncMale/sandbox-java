package stinc.male.sandbox.ratmex.meter;

import javax.annotation.concurrent.ThreadSafe;

/**
 * A lock abstraction which allows implementations with different semantics from a simple lock to a two-phase (read/write, shared/exclusive) lock.
 */
@ThreadSafe
public interface LockStrategy {
  /**
   * Non-exclusively acquires the lock if it is immediately available.
   * Implementations are free to implement this method more strictly by exclusively acquiring the lock.
   *
   * @return A stamp that can be used to {@linkplain #unlockShared(long) unlock}, or 0 if the lock is not available.
   */
  long trySharedLock();

  /**
   * Non-exclusively acquires the lock, blocking if necessary until available.
   * Implementations are free to implement this method more strictly by exclusively acquiring the lock.
   *
   * @return A stamp (not 0) that can be used to {@linkplain #unlockShared(long) unlock}.
   */
  long sharedLock();

  /**
   * If the lock state matches the given stamp, releases the non-exclusive lock.
   *
   * @param stamp A stamp returned by a non-exclusive-lock operation. Must not be 0.
   *
   * @throws IllegalMonitorStateException If the stamp is not 0 and does not match the current state of this lock.
   */
  void unlockShared(long stamp);

  /**
   * @return true if the lock is currently being held non-exclusively (or exclusively, if the implementation uses only exclusive locking),
   * false otherwise.
   */
  boolean isSharedLocked();

  /**
   * Exclusively acquires the lock if it is immediately available.
   *
   * @return A stamp that can be used to {@linkplain #unlock(long) unlock}, or 0 if the lock is not available.
   */
  long tryLock();

  /**
   * Exclusively acquires the lock, blocking if necessary until available.
   *
   * @return A stamp (not 0) that can be used to {@linkplain #unlock(long) unlock}.
   */
  long lock();

  /**
   * If the lock state matches the given stamp, releases the non-exclusive lock.
   *
   * @param stamp A stamp returned by an exclusive-lock operation. Must not be 0.
   *
   * @throws IllegalMonitorStateException If the stamp is not 0 and does not match the current state of this lock.
   */
  void unlock(long stamp);
}