package stinc.male.sandbox.ratexecutor;

import java.util.function.BooleanSupplier;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public final class YieldWaitStrategy implements WaitStrategy {
  public YieldWaitStrategy() {
  }

  @Override
  public final void await(final BooleanSupplier condition) {
    while (!condition.getAsBoolean()) {
      Thread.yield();
    }
  }
}