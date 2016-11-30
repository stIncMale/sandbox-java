package stinc.male.sandbox.ratexecutor;

import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public final class AtomicLongTicksCounter extends AbstractTicksCounter {
  private final AtomicLong aValue;

  AtomicLongTicksCounter(final long initialValue) {
    aValue = new AtomicLong(initialValue);
  }

  @Override
  public final long addAndGet(final long delta) {
    return aValue.addAndGet(delta);
  }

  @Override
  public final void add(final long delta) {
    aValue.addAndGet(delta);
  }

  @Override
  public final long get() {
    return aValue.get();
  }
}