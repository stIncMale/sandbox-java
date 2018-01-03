package stinc.male.sandbox.ratmex.meter;

/**
 * A counter of ticks that can be used by {@link RateMeter} implementations.
 */
public interface TicksCounter {
  /**
   * Changes the {@linkplain #get() value} of this counter by {@code delta}.
   */
  void add(long delta);

  /**
   * @return The value of this counter.
   */
  long get();
}