package stinc.male.sandbox.ratexecutor;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public final class LongTicksCounter extends AbstractTicksCounter {
  private long value;

  LongTicksCounter(final long initialValue) {
    value = initialValue;
  }

  @Override
  public final long addAndGet(final long delta) {
    value = value + delta;
    return value;
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