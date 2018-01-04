package stinc.male.sandbox.ratmex.meter;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;
import javax.annotation.concurrent.ThreadSafe;
import static stinc.male.sandbox.ratmex.internal.util.Preconditions.checkArgument;
import static stinc.male.sandbox.ratmex.internal.util.Preconditions.checkNotNull;

/**
 * This implementation of {@link WaitStrategy} combines exponential backoff and {@link LockSupport#park() parking}.
 */
@ThreadSafe
public final class ParkWaitStrategy implements WaitStrategy {
  private static final ParkWaitStrategy instance = new ParkWaitStrategy(50, 200);

  private final long minDelay;
  private final long maxDelay;

  /**
   * @param minDelayNanos The lower desired bound of the blocking time intervals between tests of a {@linkplain #await(BooleanSupplier) condition}.
   * Note that this is just a hint, so this implementation may use a smaller value.
   * @param maxDelayNanos The upper desired bound of the blocking time intervals between tests of a {@linkplain #await(BooleanSupplier) condition}.
   * Note that this is just a hint, so this implementation may use a larger value.
   */
  public ParkWaitStrategy(final long minDelayNanos, final long maxDelayNanos) {
    checkArgument(minDelayNanos > 0, "minDelayNanos", "Must be positive");
    checkArgument(maxDelayNanos > 0, "maxDelayNanos", "Must be positive");
    checkArgument(maxDelayNanos >= minDelayNanos, "maxDelayNanos",
        () -> String.format("Must be not less than %s=%sns", "minDelayNanos", minDelayNanos));
    this.minDelay = minDelayNanos;
    this.maxDelay = maxDelayNanos;
  }

  /**
   * Always returns the same instance with:
   * <ul>
   * <li>{@code minDelayNanos} = 50;</li>
   * <li>{@code maxDelayNanos} = 200.</li>
   * </ul>
   *
   * @return An instance of {@link ParkWaitStrategy}.
   */
  public static final ParkWaitStrategy defaultInstance() {
    return instance;
  }

  @Override
  public final void await(final BooleanSupplier condition) {
    checkNotNull(condition, "condition");
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