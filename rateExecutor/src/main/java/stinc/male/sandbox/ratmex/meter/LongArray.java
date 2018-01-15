package stinc.male.sandbox.ratmex.meter;

import java.util.Arrays;

/**
 * An array abstraction which allows implementations with different semantics.
 */
public interface LongArray {
  /**
   * @return The length of this array.
   */
  int length();

  /**
   * Sets the element at position {@code idx} to the given {@code value}.
   *
   * @param idx The index.
   * @param value The new value.
   */
  void set(int idx, long value);

  /**
   * Gets the current value at position {@code idx}.
   *
   * @param idx The index.
   * @return The current value.
   */
  long get(int idx);

  /**
   * Adds the given {@code delta} to the element at index {@code idx}.
   *
   * @param idx The index.
   * @param delta The value to add.
   */
  void add(int idx, long delta);
}