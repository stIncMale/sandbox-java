package stinc.male.sandbox.ratmex.meter;

import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.concurrent.ThreadSafe;

/**
 * This {@link TicksCounter} uses {@link AtomicLong} field to store its {@linkplain #get() value}.
 */
@ThreadSafe
public final class AtomicLongTicksCounter extends AbstractTicksCounter {
  private final AtomicLong aValue;

  public AtomicLongTicksCounter(final long initialValue) {
    aValue = new AtomicLong(initialValue);
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