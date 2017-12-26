package stinc.male.sandbox.ratmex.meter;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public final class LongTicksCounter extends AbstractTicksCounter {
  private long value;

  public LongTicksCounter(final long initialValue) {
    value = initialValue;
  }

  @Override
  public final void add(final long delta) {
    value += delta;
  }

  @Override
  public final long get() {
    return value;
  }
}