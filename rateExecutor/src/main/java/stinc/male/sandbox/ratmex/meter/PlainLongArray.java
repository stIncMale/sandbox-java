package stinc.male.sandbox.ratmex.meter;

import java.util.Arrays;
import javax.annotation.concurrent.NotThreadSafe;
import static stinc.male.sandbox.ratmex.internal.util.Preconditions.checkArgument;

/**
 * This sequential implementation of {@link LongArray} is just a wrapper over a plain {@code long[]} array.
 */
@NotThreadSafe
public final class PlainLongArray implements LongArray {
  final long[] array;

  /**
   * @param length The length of the new array. Must be positive.
   */
  public PlainLongArray(final int length) {
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

  /**
   * @return A string representation of the underlying array as per {@link Arrays#toString(long[])}.
   */
  @Override
  public final String toString() {
    return Arrays.toString(array);
  }
}