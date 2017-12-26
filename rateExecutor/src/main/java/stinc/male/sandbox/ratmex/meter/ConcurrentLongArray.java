package stinc.male.sandbox.ratmex.meter;

import java.util.concurrent.atomic.AtomicLongArray;
import stinc.male.sandbox.ratmex.util.internal.Preconditions;

final class ConcurrentLongArray implements LongArray {
  final AtomicLongArray array;

  ConcurrentLongArray(final int length) {
    Preconditions.checkArgument(length > 0, "length", "Must be positive");
    this.array = new AtomicLongArray(length);
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