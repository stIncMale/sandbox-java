package stinc.male.sandbox.ratmex.meter;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import static stinc.male.sandbox.ratmex.internal.util.Preconditions.checkArgument;
import static stinc.male.sandbox.ratmex.internal.util.Preconditions.checkNotNull;

/**
 * A configuration that can be used to create {@link AbstractRateMeter}.
 * <p>
 * The default values:
 * <ul>
 * <li>{@link #getTicksCounterSupplier()} - {@link LongTicksCounter}{@code ::}{@link LongTicksCounter#LongTicksCounter(long) new}</li>
 * <li>{@link #getTimeSensitivity()} - {@link Optional}{@code .}{@linkplain Optional#empty() empty()}</li>
 * <li>{@link #getHistoryLength()} - 2</li>
 * </ul>
 */
@Immutable
public class RateMeterConfig {
  private final Function<Long, ? extends TicksCounter> ticksCounterSupplier;
  @Nullable
  private final Duration timeSensitivity;
  private final int historyLength;

  /**
   * @param ticksCounterSupplier See {@link Builder#setTicksCounterSupplier(Function)}.
   * @param timeSensitivity See {@link Builder#setTimeSensitivity(Duration)}.
   * @param historyLength See {@link Builder#setHistoryLength(int)}.
   */
  protected RateMeterConfig(
      final Function<Long, ? extends TicksCounter> ticksCounterSupplier,
      @Nullable final Duration timeSensitivity,
      final int historyLength) {
    checkNotNull(ticksCounterSupplier, "ticksCounterSupplier");
    if (timeSensitivity != null) {
      checkArgument(!timeSensitivity.isNegative(), "timeSensitivity", "Must be positive");
      checkArgument(!timeSensitivity.isZero(), "timeSensitivity", "Must not be zero");
    }
    checkArgument(historyLength >= 2, "historyLength", "Must be greater than or equal to 2");
    this.ticksCounterSupplier = ticksCounterSupplier;
    this.timeSensitivity = timeSensitivity;
    this.historyLength = historyLength;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public Builder toBuilder() {
    return new Builder().set(this);
  }

  /**
   * Specifies a supplier which must be used by {@link AbstractRateMeter} to create ticks counters.
   * Note that if {@link AbstractRateMeter} is used concurrently, then the supplier must provide a thread-safe {@link TicksCounter}.
   * If this constraint is violated, then the behavior is unspecified.
   */
  public final Function<Long, ? extends TicksCounter> getTicksCounterSupplier() {
    return ticksCounterSupplier;
  }

  /**
   * An {@linkplain Optional#empty() empty} time sensitivity means that {@link AbstractRateMeter}
   * will automatically use {@linkplain RateMeter#getSamplesInterval() samples interval} / 20 as the time sensitivity,
   * which in turn means that the samples interval must be a multiple of 20.
   *
   * @see RateMeter#getTimeSensitivity()
   */
  public final Optional<Duration> getTimeSensitivity() {
    return Optional.ofNullable(timeSensitivity);
  }

  /**
   * Specifies the length of samples history measured in {@linkplain RateMeter#getSamplesInterval() samplesInterval} units.
   * Actual samples history duration maintained by {@link RateMeter} must be within
   * [historyLength * {@linkplain RateMeter#getSamplesInterval() samplesInterval};
   * (historyLength + 1) * {@linkplain RateMeter#getSamplesInterval() samplesInterval}].
   * <p>
   * Note that the specification of {@link RateMeter} states that any {@link RateMeter}
   * must maintain samples history for at least 2{@linkplain RateMeter#getSamplesInterval() samplesInterval}.
   * <p>
   * The longer history a {@link RateMeter} maintains, the less likely a measurement can be {@linkplain RateMeterReading#isAccurate() inaccurate},
   * but the more memory it occupies. This might also negatively affect performance;
   * for example the performance of {@link AbstractNavigableMapRateMeter} degrades as the length of the history grows,
   * while {@link AbstractRingBufferRateMeter} can tolerate an arbitrary long history.
   */
  public final int getHistoryLength() {
    return historyLength;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() +
        "{ticksCounterSupplier=" + ticksCounterSupplier +
        ", timeSensitivity=" + timeSensitivity +
        ", historyLength=" + historyLength +
        '}';
  }

  @NotThreadSafe
  public static class Builder {
    protected Function<Long, ? extends TicksCounter> ticksCounterSupplier;
    @Nullable
    protected Duration timeSensitivity;
    protected int historyLength;

    protected Builder() {
      ticksCounterSupplier = LongTicksCounter::new;
      timeSensitivity = null;
      historyLength = 2;
    }

    /**
     * @param config Must not be null.
     */
    public final Builder set(final RateMeterConfig config) {
      checkNotNull(config, "config");
      ticksCounterSupplier = config.getTicksCounterSupplier();
      timeSensitivity = config.getTimeSensitivity()
          .orElse(null);
      historyLength = config.getHistoryLength();
      return this;
    }

    /**
     * @param ticksCounterSupplier Must be immutable.
     *
     * @see RateMeterConfig#getTicksCounterSupplier()
     */
    public final Builder setTicksCounterSupplier(final Function<Long, ? extends TicksCounter> ticksCounterSupplier) {
      checkNotNull(ticksCounterSupplier, "ticksCounterSupplier");
      this.ticksCounterSupplier = ticksCounterSupplier;
      return this;
    }

    /**
     * @param timeSensitivity Must be either null or positive
     * (not {@linkplain Duration#isNegative() negative} and not {@linkplain Duration#isZero() zero}).
     *
     * @see RateMeterConfig#getTimeSensitivity()
     */
    public final Builder setTimeSensitivity(@Nullable final Duration timeSensitivity) {
      if (timeSensitivity != null) {
        checkArgument(!timeSensitivity.isNegative(), "timeSensitivity", "Must be positive");
        checkArgument(!timeSensitivity.isZero(), "timeSensitivity", "Must not be zero");
      }
      this.timeSensitivity = timeSensitivity;
      return this;
    }

    /**
     * @param historyLength Must be greater than or equal to 2.
     *
     * @see RateMeterConfig#getHistoryLength()
     */
    public final Builder setHistoryLength(final int historyLength) {
      checkArgument(historyLength >= 2, "historyLength", "Must be greater than or equal to 2");
      this.historyLength = historyLength;
      return this;
    }

    public RateMeterConfig build() {
      return new RateMeterConfig(
          ticksCounterSupplier,
          timeSensitivity,
          historyLength);
    }
  }
}