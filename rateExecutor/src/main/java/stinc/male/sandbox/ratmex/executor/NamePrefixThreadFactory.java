package stinc.male.sandbox.ratmex.executor;

import java.util.concurrent.ThreadFactory;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import static stinc.male.sandbox.ratmex.internal.util.Preconditions.checkNotNull;

@ThreadSafe
final class NamePrefixThreadFactory implements ThreadFactory {
  private final ThreadFactory threadFactory;
  private final String namePrefix;

  NamePrefixThreadFactory(final ThreadFactory threadFactory, final String namePrefix) {
    checkNotNull(threadFactory, "threadFactory");
    checkNotNull(namePrefix, "namePrefix");
    this.threadFactory = threadFactory;
    this.namePrefix = namePrefix;
  }

  @Nullable
  @Override
  public final Thread newThread(final Runnable r) {
    @Nullable
    final Thread result = threadFactory.newThread(r);
    if (result != null) {
      result.setName(namePrefix + result.getName());
    }
    return result;
  }
}