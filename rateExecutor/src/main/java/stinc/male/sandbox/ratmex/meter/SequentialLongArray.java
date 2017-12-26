package stinc.male.sandbox.ratmex.meter;

import stinc.male.sandbox.ratmex.util.internal.Preconditions;

final class SequentialLongArray implements LongArray {
  final long[] array;

  SequentialLongArray(final int length) {
    Preconditions.checkArgument(length > 0, "length", "Must be positive");
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