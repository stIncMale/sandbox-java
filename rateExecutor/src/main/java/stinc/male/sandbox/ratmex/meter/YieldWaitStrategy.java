package stinc.male.sandbox.ratmex.meter;

import java.util.function.BooleanSupplier;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public final class YieldWaitStrategy implements WaitStrategy {
  private static final YieldWaitStrategy instance = new YieldWaitStrategy();

  /**
   * Always returns the same instance.
   *
   * @return An instance of {@link YieldWaitStrategy}.
   */
  public static final YieldWaitStrategy instance() {
    return instance;
  }

  @Override
  public final void await(final BooleanSupplier condition) {
    while (!condition.getAsBoolean()) {
      Thread.onSpinWait();
      Thread.yield();
    }
  }

  private YieldWaitStrategy() {
  }
}