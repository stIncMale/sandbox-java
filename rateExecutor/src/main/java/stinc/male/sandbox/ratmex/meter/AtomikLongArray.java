package stinc.male.sandbox.ratmex.meter;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLongArray;
import javax.annotation.concurrent.ThreadSafe;
import static stinc.male.sandbox.ratmex.internal.util.Preconditions.checkArgument;

/**
 * This concurrent implementation of {@link LongArray} is just a wrapper over an {@link AtomicLongArray}.
 * It is named {@link AtomikLongArray} with {@code 'k'} in order to avoid a name clash.
 */
@ThreadSafe
public final class AtomikLongArray implements LongArray {
  final AtomicLongArray array;

  /**
   * @param length The length of the new array. Must be positive.
   */
  public AtomikLongArray(final int length) {
    checkArgument(length > 0, "length", "Must be positive");
    this.array = new AtomicLongArray(length);
  }

  /**
   * @return See {@link AtomicLongArray#length()}.
   */
  @Override
  public final int length() {
    return array.length();
  }

  /**
   * @return See {@link AtomicLongArray#set(int, long)}.
   */
  @Override
  public final void set(final int idx, final long value) {
    array.set(idx, value);
  }

  /**
   * @return See {@link AtomicLongArray#get(int)}.
   */
  @Override
  public final long get(int idx) {
    return array.get(idx);
  }

  /**
   * @return See {@link AtomicLongArray#getAndAdd(int, long)}.
   */
  @Override
  public final void add(final int idx, final long delta) {
    array.getAndAdd(idx, delta);
  }

  /**
   * @return See {@link AtomicLongArray#toString()}.
   */
  @Override
  public final String toString() {
    return array.toString();
  }
}