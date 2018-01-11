package stinc.male.sandbox.ratmex.meter;

import java.util.concurrent.atomic.AtomicLongArray;
import javax.annotation.concurrent.ThreadSafe;
import static stinc.male.sandbox.ratmex.internal.util.Preconditions.checkArgument;

@ThreadSafe
final class AtomikLongArray implements LongArray {//TODO mention in doc why spell Atomik (because of conflict with JDK AtomicLongArray)
  final AtomicLongArray array;

  AtomikLongArray(final int length) {
    checkArgument(length > 0, "length", "Must be positive");
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