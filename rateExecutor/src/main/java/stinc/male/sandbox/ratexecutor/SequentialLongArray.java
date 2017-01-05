package stinc.male.sandbox.ratexecutor;

final class SequentialLongArray implements LongArray {
  final long[] array;

  SequentialLongArray(final long[] array) {
    Preconditions.checkNotNull(array, "array");
    this.array = array;
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