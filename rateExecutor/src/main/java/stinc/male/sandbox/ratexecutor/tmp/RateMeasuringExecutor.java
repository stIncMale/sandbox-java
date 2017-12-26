package stinc.male.sandbox.ratexecutor.tmp;

import java.time.Duration;
import java.util.concurrent.Future;
import javax.annotation.concurrent.ThreadSafe;
import stinc.male.sandbox.ratexecutor.RateMeter;

/**
 * RatMeX.
 */
@ThreadSafe
public interface RateMeasuringExecutor extends AutoCloseable {
  /**
   *
   * @param task
   * @param rateMeter must be {@link ThreadSafe}. Must not contain any ticks and must have its startNanos not after start = now + delay
   * initialDelay
   * @param rate
   * @return
   */
  Future<?> submit(Runnable task, RateMeter rateMeter, Rate rate, Duration delay);

  void shutdown(boolean shutdownWorker);

  default void close() {
    shutdown(true);
  }
}