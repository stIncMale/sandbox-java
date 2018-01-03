package stinc.male.sandbox.ratmex.meter;

/**
 * An abstract {@link TicksCounter}, which is a good start for a {@link TicksCounter} implementation.
 */
public abstract class AbstractTicksCounter implements TicksCounter {
  protected AbstractTicksCounter() {
  }

  @Override
  public String toString() {
    return String.valueOf(get());
  }
}