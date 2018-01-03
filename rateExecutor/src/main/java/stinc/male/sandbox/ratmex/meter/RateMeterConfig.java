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
 *
 * The {@code @}{@link Immutable} for this class only guarantees that {@code get...} methods behave as methods of an immutable class
 * despite this class is not final.
 */
@Immutable
public class RateMeterConfig {
  private final Function<Long, ? extends TicksCounter> ticksCounterSupplier;
  @Nullable
  private final Duration timeSensitivity;
  private final int maxTicksCountAttempts;//TODO move to concurrent
  private final int historyLength;

  protected RateMeterConfig(
      final Function<Long, ? extends TicksCounter> ticksCounterSupplier,
      @Nullable final Duration timeSensitivity,
      final int maxTicksCountAttempts,
      final int historyLength) {
    this.ticksCounterSupplier = ticksCounterSupplier;
    this.timeSensitivity = timeSensitivity;
    this.maxTicksCountAttempts = maxTicksCountAttempts;
    this.historyLength = historyLength;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public Builder toBuilder() {
    return new Builder(this);
  }

  /**
   * Specifies a supplier which MUST be used by {@link AbstractRateMeter} to create ticks counters.
   * Note that if {@link AbstractRateMeter} is used concurrently
   * then supplier MUST provide a thread-safe implementation of {@link TicksCounter}.
   *
   * @return {@code LongAdderTicksCounter::new} by default.
   */
  public final Function<Long, ? extends TicksCounter> getTicksCounterSupplier() {
    return ticksCounterSupplier;
  }

  /**
   * An {@linkplain Optional#empty() empty} time sensitivity means that {@link AbstractRateMeter}
   * will automatically use {@linkplain RateMeter#getSamplesInterval() samples interval} / 20 as the time sensitivity,
   * which in turn means that the samples interval must be a multiple of 20.
   *
   * @return An {@linkplain Optional#empty() empty} {@link Optional} by default.
   *
   * @see RateMeter#getTimeSensitivity()
   */
  public final Optional<Duration> getTimeSensitivity() {
    return Optional.ofNullable(timeSensitivity);
  }

  /**
   * Specifies the desired maximum number of attempts to calculate the number of ticks (see {@link RateMeter#ticksCount()} for example).
   * Note that this is just a hint, so an implementation may choose to do more attempts, but the number of attempts must be finite.
   * <p>
   * <b>The reasoning behind this hint</b><br>
   * Implementations may allow a race condition (for performance reasons) while counting ticks.
   * When running out of the number of attempts such implementations may choose to fall over to an approach that excludes the race
   * and allows to eventually count the ticks.
   *
   * @return 6 by default.
   */
  public final int getMaxTicksCountAttempts() {
    return maxTicksCountAttempts;
  }

  /**
   * Specifies the length of samples history measured in {@linkplain RateMeter#getSamplesInterval() samplesInterval} units.
   * Actual samples history duration maintained by {@link RateMeter} must be within
   * [historyLength * {@linkplain RateMeter#getSamplesInterval() samplesInterval};
   * (historyLength + 1) * {@linkplain RateMeter#getSamplesInterval() samplesInterval}].
   * <p>
   * Note that the specification of {@link RateMeter#rate(long)} implies that any {@link RateMeter}
   * must maintain samples history for at least 2{@linkplain RateMeter#getSamplesInterval() samplesInterval}.
   *
   * @return 3 by default.
   */
  public final int getHistoryLength() {
    return historyLength;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "{ticksCounterSupplier=" + ticksCounterSupplier
        + ", timeSensitivity=" + timeSensitivity
        + ", maxTicksCountAttempts=" + maxTicksCountAttempts
        + ", historyLength=" + historyLength
        + '}';
  }

  @NotThreadSafe
  public static class Builder {
    protected Function<Long, ? extends TicksCounter> ticksCounterSupplier;
    @Nullable
    protected Duration timeSensitivity;
    protected int maxTicksCountAttempts;
    protected int historyLength;

    protected Builder() {
      ticksCounterSupplier = LongAdderTicksCounter::new;
      timeSensitivity = null;
      maxTicksCountAttempts = 6;
      historyLength = 3;
    }

    protected Builder(final RateMeterConfig config) {
      ticksCounterSupplier = config.getTicksCounterSupplier();
      timeSensitivity = config.getTimeSensitivity()
          .orElse(null);
      maxTicksCountAttempts = config.getMaxTicksCountAttempts();
      historyLength = config.getHistoryLength();
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
     * @param maxTicksCountAttempts Must be positive.
     *
     * @see RateMeterConfig#getMaxTicksCountAttempts()
     */
    public final Builder setMaxTicksCountAttempts(final int maxTicksCountAttempts) {
      checkArgument(maxTicksCountAttempts > 0, "maxTicksCountAttempts", "Must be positive");
      this.maxTicksCountAttempts = maxTicksCountAttempts;
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
          maxTicksCountAttempts,
          historyLength);
    }
  }
}