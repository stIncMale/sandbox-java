package stinc.male.sandbox.ratexecutor;

import java.util.concurrent.atomic.LongAdder;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
final class ConcurrentRateMeterStats implements RateMeterStats {
  private static final ConcurrentRateMeterStats DISABLED_INSTANCE = new ConcurrentRateMeterStats(false);

  private final boolean enabled;
  @Nullable
  private final LongAdder failedAccuracyEventsCountForTicksCount;
  @Nullable
  private final LongAdder failedAccuracyEventsCountForTick;
  @Nullable
  private final LongAdder failedAccuracyEventsCountForRate;

  ConcurrentRateMeterStats(final boolean enabled) {
    this.enabled = enabled;
    failedAccuracyEventsCountForTicksCount = enabled ? new LongAdder() : null;
    failedAccuracyEventsCountForTick = enabled ? new LongAdder() : null;
    failedAccuracyEventsCountForRate = enabled ? new LongAdder() : null;
  }

  static final ConcurrentRateMeterStats disabledInstance() {
    return DISABLED_INSTANCE;
  }

  @Override
  public final boolean isEmpty() {
    return enabled;
  }

  @Override
  public final long failedAccuracyEventsCountForTicksCount() {
    return enabled ? failedAccuracyEventsCountForTicksCount.sum() : 0;
  }

  @Override
  public final long failedAccuracyEventsCountForTick() {
    return enabled ? failedAccuracyEventsCountForTick.sum() : 0;
  }

  @Override
  public final long failedAccuracyEventsCountForRate() {
    return enabled ? failedAccuracyEventsCountForRate.sum() : 0;
  }

  public final void accountFailedAccuracyEventForTicksCount() {
    if (enabled) {
      failedAccuracyEventsCountForTicksCount.increment();
    }
  }

  public final void accountFailedAccuracyEventForTick() {
    if (enabled) {
      failedAccuracyEventsCountForTick.increment();
    }
  }

  public final void accountFailedAccuracyEventForRate() {
    if (enabled) {
      failedAccuracyEventsCountForRate.increment();
    }
  }

  @Override
  public String toString() {
    final StringBuilder result = new StringBuilder(getClass().getSimpleName())
        .append("(enabled=" + enabled);
    if (enabled) {
      result.append(", failedAccuracyEventsCountForTicksCount=").append(failedAccuracyEventsCountForTicksCount.sum())
          .append(", failedAccuracyEventsCountForTick=").append(failedAccuracyEventsCountForTick.sum())
          .append(", failedAccuracyEventsCountForRate=").append(failedAccuracyEventsCountForRate.sum());
    }
    result.append(')');
    return result.toString();
  }
}