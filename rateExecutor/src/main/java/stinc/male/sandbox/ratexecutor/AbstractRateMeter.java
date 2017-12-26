package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import stinc.male.sandbox.ratexecutor.util.internal.ConversionsAndChecks;
import stinc.male.sandbox.ratexecutor.util.internal.Preconditions;
import static stinc.male.sandbox.ratexecutor.util.internal.Preconditions.checkNotNull;
import static stinc.male.sandbox.ratexecutor.util.internal.ConversionsAndChecks.convertRate;

abstract class AbstractRateMeter<C extends RateMeterConfig> implements ConfigurableRateMeter<C> {
  private final TicksCounter ticksTotal;
  private final long startNanos;
  private final Duration samplesInterval;
  private final long samplesIntervalNanos;
  private final long maxTNanos;
  private final ConcurrentRateMeterStats stats;
  private final C config;

  /**
   * @param startNanos Starting point that is used to calculate elapsed nanoseconds.
   * @param samplesInterval Size of the samples window.
   * @param config Additional configuration parameters.
   */
  AbstractRateMeter(final long startNanos, final Duration samplesInterval, final C config) {
    checkNotNull(samplesInterval, "samplesInterval");
    Preconditions.checkArgument(!samplesInterval.isZero(), "samplesInterval", "Must not be zero");
    Preconditions.checkArgument(!samplesInterval.isNegative(), "samplesInterval", "Must be positive");
    checkNotNull(config, "config");
    this.startNanos = startNanos;
    this.samplesInterval = samplesInterval;
    samplesIntervalNanos = samplesInterval.toNanos();
    Preconditions.checkArgument(samplesIntervalNanos <= Long.MAX_VALUE / (config.getHl() + 1) - 1, "samplesInterval",
        () -> String.format("Must be less than (Long.MAX_VALUE - 1)nanos = %snanos, but actual value is %s", Long.MAX_VALUE - 1, samplesIntervalNanos));
    maxTNanos = ConversionsAndChecks.maxTNanos(startNanos, samplesIntervalNanos, config.getHl() + 1);
    this.config = config;
    ticksTotal = config.getTicksCounterSupplier().apply(0L);
    stats = config.isCollectStats() ? new ConcurrentRateMeterStats(true) : ConcurrentRateMeterStats.disabledInstance();
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
  public double rateAverage(final Duration unit) {
    checkArgument(unit, "unit");
    return convertRate(rateAverage(), samplesIntervalNanos, unit.toNanos());
  }

  @Override
  public double rateAverage(final long tNanos, final Duration unit) {
    checkArguments(tNanos, "tNanos", unit, "unit");
    return convertRate(rateAverage(tNanos), samplesIntervalNanos, unit.toNanos());
  }

  @Override
  public double rate(final Duration unit) {
    checkArgument(unit, "unit");
    return convertRate(rate(), samplesIntervalNanos, unit.toNanos());
  }

  @Override
  public RateMeterReading rate(final Duration unit, final RateMeterReading reading) {
    checkArgument(unit, "unit");
    checkNotNull(reading, "reading");
    return convertRate(rate(reading), getSamplesInterval().toNanos(), unit.toNanos());
  }

  @Override
  public double rate(final long tNanos, final Duration unit) {
    checkArguments(tNanos, "tNanos", unit, "unit");
    return convertRate(rate(tNanos), samplesIntervalNanos, unit.toNanos());
  }

  @Override
  public RateMeterReading rate(final long tNanos, final Duration unit, final RateMeterReading reading) {
    checkArguments(tNanos, "tNanos", unit, "unit");
    checkNotNull(reading, "reading");
    return convertRate(rate(tNanos, reading), samplesIntervalNanos, unit.toNanos());
  }

  @Override
  public final RateMeterStats stats() {
    return stats;
  }

  @Override
  public final C getConfig() {
    return config;
  }

  protected final ConcurrentRateMeterStats getStats() {
    return stats;
  }

  protected final TicksCounter getTicksTotalCounter() {
    return ticksTotal;
  }

  /**
   * @return {@link #getSamplesInterval()} in nanoseconds.
   */
  protected final long getSamplesIntervalNanos() {
    return samplesIntervalNanos;
  }

  protected final void checkArgument(final long tNanos, final String safeParamName) throws IllegalArgumentException {
    ConversionsAndChecks.checkTNanos(tNanos, startNanos, maxTNanos, safeParamName);
  }

  protected final void checkArgument(final Duration unit, final String safeUnitParamName) throws IllegalArgumentException {
    ConversionsAndChecks.checkUnit(unit, safeUnitParamName);
  }

  protected final void checkArguments(
      final long tNanos, final String safeTNanosParamName,
      final Duration unit, final String safeUnitParamName) throws IllegalArgumentException {
    ConversionsAndChecks.checkTNanos(tNanos, startNanos, maxTNanos, safeTNanosParamName);
    ConversionsAndChecks.checkUnit(unit, safeUnitParamName);
  }

  @Override
  public String toString() {//TODO replace ( with { everywhere in toString
    return getClass().getSimpleName()
        + "(startNanos=" + startNanos
        + ", samplesIntervalNanos=" + samplesIntervalNanos
        + ", config=" + config
        + ", stats=" + stats
        + ')';
  }
}