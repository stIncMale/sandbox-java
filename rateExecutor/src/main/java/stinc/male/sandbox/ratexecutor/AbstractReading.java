package stinc.male.sandbox.ratexecutor;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
class AbstractReading {
  long tNanos;
  boolean accurate;

  public final long getTNanos() {
    return tNanos;
  }

  public final boolean isAccurate() {
    return accurate;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "(tNanos=" + tNanos
        + ", accurate=" + accurate
        + ')';
  }
}