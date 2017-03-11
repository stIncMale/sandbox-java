package stinc.male.sandbox.ratexecutor;

import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;
import javax.annotation.concurrent.ThreadSafe;
import static stinc.male.sandbox.ratexecutor.Preconditions.checkArgument;

@ThreadSafe
public final class ParkWaitStrategy implements WaitStrategy {
  private static final ParkWaitStrategy instance = new ParkWaitStrategy(1);

  private final long parkNanos;

  public ParkWaitStrategy(final long parkNanos) {
    checkArgument(parkNanos > 0, "parkNanos", "Must be positive");
    this.parkNanos = parkNanos;
  }

  /**
   * Always returns the same instance.
   *
   * @return An instance of {@link ParkWaitStrategy}.
   */
  public static final ParkWaitStrategy instance() {
    return instance;
  }

  @Override
  public final void await(final BooleanSupplier condition) {
    boolean interrupted = false;
    try {
      while (!condition.getAsBoolean()) {
        LockSupport.parkNanos(1);
        if (Thread.interrupted()) {
          interrupted = true;
        }
      }
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }
}