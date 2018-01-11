package stinc.male.sandbox.ratmex.meter;

import javax.annotation.concurrent.NotThreadSafe;
import static stinc.male.sandbox.ratmex.internal.util.Preconditions.checkArgument;

@NotThreadSafe
final class PlainLongArray implements LongArray {//TODO make public (add docs) because abstract classes are also marked TODO make public
  final long[] array;

  PlainLongArray(final int length) {
    checkArgument(length > 0, "length", "Must be positive");
    this.array = new long[length];
  }

  @Override
  public final int length() {
    return array.length;
  }

  @Override
  public final void set(final int idx, final long value) {
    array[idx] = value;
  }

  @Override
  public final long get(int idx) {
    return array[idx];
  }

  @Override
  public final void add(final int idx, final long delta) {
    array[idx] += delta;
  }
}