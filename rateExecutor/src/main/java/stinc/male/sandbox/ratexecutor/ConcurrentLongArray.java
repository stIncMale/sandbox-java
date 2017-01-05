package stinc.male.sandbox.ratexecutor;

import java.util.concurrent.atomic.AtomicLongArray;

final class ConcurrentLongArray implements LongArray {
  final AtomicLongArray array;

  ConcurrentLongArray(final AtomicLongArray array) {
    Preconditions.checkNotNull(array, "array");
    this.array = array;
  }

  @Override
  public final int length() {
    return array.length();
  }

  @Override
  public final void set(final int idx, final long value) {
    array.set(idx, value);
  }

  @Override
  public final long get(int idx) {
    return array.get(idx);
  }

  @Override
  public final void add(final int idx, final long delta) {
    array.getAndAdd(idx, delta);
  }
}