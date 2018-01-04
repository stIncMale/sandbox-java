package stinc.male.sandbox.ratmex.meter;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;
import javax.annotation.concurrent.ThreadSafe;
import static stinc.male.sandbox.ratmex.internal.util.Preconditions.checkArgument;

@ThreadSafe
public final class ParkWaitStrategy implements WaitStrategy {
  private static final ParkWaitStrategy instance = new ParkWaitStrategy(50, 200);

  private final long minDelay;
  private final long maxDelay;

  public ParkWaitStrategy(final long minDelayNanos, final long maxDelayNanos) {
    checkArgument(minDelayNanos > 0, "minDelayNanos", "Must be positive");
    checkArgument(maxDelayNanos > 0, "maxDelayNanos", "Must be positive");
    checkArgument(maxDelayNanos >= minDelayNanos, "maxDelayNanos",
        () -> String.format("Must be not less than %s=%sns", "minDelayNanos", minDelayNanos));
    this.minDelay = minDelayNanos;
    this.maxDelay = maxDelayNanos;
  }

  /**
   * Always returns the same instance.
   *
   * @return An instance of {@link ParkWaitStrategy}.
   */
  public static final ParkWaitStrategy defaultInstance() {
    return instance;
  }

  @Override
  public final void await(final BooleanSupplier condition) {
    boolean interrupted = false;
    long delayNanos = minDelay;
    final ThreadLocalRandom rnd = ThreadLocalRandom.current();
    try {
      while (!condition.getAsBoolean()) {
        Thread.onSpinWait();
        LockSupport.parkNanos(rnd.nextLong(delayNanos));
        if (delayNanos < maxDelay) {
          delayNanos *= 2;
        }
        if (Thread.interrupted()) {
          interrupted = true;
        }
      }
    } finally {
      if (interrupted) {
        Thread.currentThread()
            .interrupt();
      }
    }
  }
}