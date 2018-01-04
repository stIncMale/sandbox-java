package stinc.male.sandbox.ratmex.meter;

import java.util.function.BooleanSupplier;
import javax.annotation.concurrent.ThreadSafe;

/**
 * An abstraction which allows implementations of different approaches to block on a condition.
 */
@ThreadSafe
public interface WaitStrategy {
  /**
   * This method blocks until the {@code condition} is true.
   *
   * @param condition A boolean condition.
   */
  void await(BooleanSupplier condition);
}