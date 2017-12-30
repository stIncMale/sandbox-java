package stinc.male.sandbox.ratmex.meter;

import java.time.Duration;
import java.util.Optional;
import javax.annotation.Nullable;
import stinc.male.sandbox.ratmex.Configurable;
import stinc.male.sandbox.ratmex.internal.util.Preconditions;
import static stinc.male.sandbox.ratmex.internal.util.ConversionsAndChecks.checkTNanos;
import static stinc.male.sandbox.ratmex.internal.util.ConversionsAndChecks.checkUnit;
import static stinc.male.sandbox.ratmex.internal.util.ConversionsAndChecks.maxTNanos;
import static stinc.male.sandbox.ratmex.internal.util.Preconditions.checkNotNull;
import static stinc.male.sandbox.ratmex.internal.util.ConversionsAndChecks.convertRate;

/**
 * A generic implementation of a {@linkplain Configurable configurable} {@link RateMeter}.
 *
 * @param <C> A type of a configuration.
 */
abstract class AbstractRateMeter<C extends RateMeterConfig> implements RateMeter, Configurable<C> {
  private final TicksCounter ticksTotal;
  private final long startNanos;
  private final Duration samplesInterval;
  private final long samplesIntervalNanos;
  private final long maxTNanos;
  @Nullable
  private final ConcurrentRateMeterStats stats;
  private final C config;

  /**
   * @param startNanos A {@linkplain #getStartNanos() starting point} that is used to calculate elapsed time in nanoseconds (tNanos).
   * @param samplesInterval A size of the {@linkplain #getSamplesInterval() samples window}.
   * Must not be null, see {@link RateMeter} for valid values.
   * @param config An additional {@linkplain #getConfig() configuration}. Must not be null.
   */
  AbstractRateMeter(final long startNanos, final Duration samplesInterval, final C config) {
    checkNotNull(samplesInterval, "samplesInterval");
    Preconditions.checkArgument(!samplesInterval.isZero(), "samplesInterval", "Must not be zero");
    Preconditions.checkArgument(!samplesInterval.isNegative(), "samplesInterval", "Must be positive");
    checkNotNull(config, "config");
    this.startNanos = startNanos;
    this.samplesInterval = samplesInterval;
    samplesIntervalNanos = samplesInterval.toNanos();
    Preconditions.checkArgument(samplesIntervalNanos <= Long.MAX_VALUE / (config.getHistoryLength() + 1) - 1, "samplesInterval",
        () -> String.format(
            "Must be less than (Long.MAX_VALUE - 1)nanos = %snanos, but actual value is %s",
            Long.MAX_VALUE - 1,
            samplesIntervalNanos));
    maxTNanos = maxTNanos(startNanos, samplesIntervalNanos, config.getHistoryLength() + 1);
    this.config = config;
    ticksTotal = config.getTicksCounterSupplier()
        .apply(0L);
    stats = config.isCollectStats() ? new ConcurrentRateMeterStats() : null;
  }

  @Override
  public final long getStartNanos() {
    return startNanos;
  }

  @Override
  public final Duration getSamplesInterval() {
    return samplesInterval;
  }

  @Override
  public final Duration getTimeSensitivity() {
    return config.getTimeSensitivity();
  }

  @Override
  public final long ticksTotalCount() {
    return ticksTotal.get();
  }

  @Override
  public final double rateAverage(final long tNanos, final Duration unit) {
    checkArguments(tNanos, "tNanos", unit, "unit");
    return convertRate(rateAverage(tNanos), samplesIntervalNanos, unit.toNanos());
  }

  @Override
  public final double rate(final long tNanos, final Duration unit) {
    checkArguments(tNanos, "tNanos", unit, "unit");
    return convertRate(rate(tNanos), samplesIntervalNanos, unit.toNanos());
  }

  @Override
  public final Optional<RateMeterStats> stats() {
    return Optional.ofNullable(stats);
  }

  @Override
  public final C getConfig() {
    return config;
  }

  @Nullable
  protected final ConcurrentRateMeterStats getStats() {
    return stats;
  }

  /**
   * @return A counter that must be used to register and calculate total ticks count.
   * {@link #ticksTotalCount()} returns {@link #getTicksTotalCounter()}{@code .}{@link TicksCounter#get() get()}.
   */
  protected final TicksCounter getTicksTotalCounter() {
    return ticksTotal;
  }

  /**
   * @return {@link #getSamplesInterval()} in nanoseconds.
   * Should be used instead of {@link #getSamplesInterval()}{@code .}{@link Duration#toNanos() toNanos()} for better performance.
   */
  protected final long getSamplesIntervalNanos() {
    return samplesIntervalNanos;
  }

  /**
   * This method should be used to check {@code tNanos} argument in methods such as {@link #rate(long)}, etc.
   *
   * @param tNanos A checked argument.
   * @param safeParamName A parameter name that will be used as is without any checks.
   *
   * @throws IllegalArgumentException If the argument is invalid.
   */
  protected final void checkArgument(final long tNanos, final String safeParamName) throws IllegalArgumentException {
    checkTNanos(tNanos, startNanos, maxTNanos, safeParamName);
  }

  /**
   * This method should be used to check {@code unit} argument in methods such as {@link #rate(Duration)}, etc.
   *
   * @param unit A checked argument.
   * @param safeParamName A parameter name that will be used as is without any checks.
   *
   * @throws IllegalArgumentException If the argument is invalid.
   */
  protected final void checkArgument(final Duration unit, final String safeParamName) throws IllegalArgumentException {
    checkUnit(unit, safeParamName);
  }

  /**
   * This method should be used to check both {@code tNanos} and {@code unit} arguments in methods such as {@link #rate(long, Duration)}, etc.
   * The method sequentially invokes methods
   * {@link #checkArgument(long, String)}, {@link #checkArgument(Duration, String)}.
   *
   * @param tNanos A checked argument.
   * @param safeTNanosParamName A {@code tNanos} parameter name that will be used as is without any checks.
   * @param unit A checked argument.
   * @param safeUnitParamName A {@code unit} parameter name that will be used as is without any checks.
   *
   * @throws IllegalArgumentException If either argument is invalid.
   */
  protected final void checkArguments(
      final long tNanos, final String safeTNanosParamName,
      final Duration unit, final String safeUnitParamName) throws IllegalArgumentException {
    checkTNanos(tNanos, startNanos, maxTNanos, safeTNanosParamName);
    checkUnit(unit, safeUnitParamName);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "{startNanos=" + startNanos
        + ", samplesIntervalNanos=" + samplesIntervalNanos
        + ", config=" + config
        + ", stats=" + stats
        + '}';
  }
}