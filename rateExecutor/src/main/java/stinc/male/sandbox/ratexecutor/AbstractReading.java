package stinc.male.sandbox.ratexecutor;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
class AbstractReading {
  long tNanos;

  public final long getTNanos() {
    return tNanos;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "(tNanos=" + tNanos + ')';
  }
}