package stinc.male.sandbox.ratmex.executor;

import java.time.Duration;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import static stinc.male.sandbox.ratmex.internal.util.ConversionsAndChecks.checkDuration;
import static stinc.male.sandbox.ratmex.internal.util.Preconditions.checkArgument;

/**
 * A configuration of a {@linkplain RateMeasuringExecutorService#scheduleAtFixedRate(Runnable, Rate, ScheduleConfig) scheduled task}.
 */
@Immutable
public class ScheduleConfig {
  private final Duration delay;
  @Nullable
  private final Duration duration;
  @Nullable
  private final RateListener listener;

  protected ScheduleConfig(
      final Duration delay,
      @Nullable final Duration duration,
      @Nullable final RateListener listener) {
    checkDuration(delay, "delay");
    if (duration != null) {
      checkArgument(!duration.isZero(), "duration", "Must not be zero");
      checkDuration(delay, "delay");
    }
    this.delay = delay;
    this.duration = duration;
    this.listener = listener;
  }
}