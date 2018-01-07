package stinc.male.sandbox.ratmex.executor;

import javax.annotation.concurrent.Immutable;

/**
 * A configuration of a {@linkplain RateMeasuringExecutorService#scheduleAtFixedRate(Runnable, Rate, ScheduleConfig) scheduled task}.
 */
@Immutable
public class ScheduleConfig {
  protected ScheduleConfig() {
  }
}