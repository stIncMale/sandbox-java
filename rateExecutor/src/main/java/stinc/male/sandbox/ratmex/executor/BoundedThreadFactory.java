package stinc.male.sandbox.ratmex.executor;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
final class BoundedThreadFactory implements ThreadFactory {
  @Nullable
  private final ThreadFactory factory;
  private final int max;
  private final AtomicInteger counter;

  BoundedThreadFactory(@Nullable final ThreadFactory factory, final int maxThreads) {
    this.factory = factory;
    max = maxThreads;
    counter = new AtomicInteger();
  }

  @Nullable
  @Override
  public final Thread newThread(@Nullable final Runnable r) {
    @Nullable
    final Thread result;
    final int currentCount = counter.get();
    if (currentCount < max && counter.compareAndSet(currentCount, currentCount + 1)) {//omit CAS when possible (similar to DCL idiom)
      result = factory == null ? new Thread(r) : factory.newThread(r);
    } else {
      result = null;
    }
    return result;
  }
}