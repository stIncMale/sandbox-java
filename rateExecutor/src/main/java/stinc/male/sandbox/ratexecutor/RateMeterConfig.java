package stinc.male.sandbox.ratexecutor;

import java.util.function.Function;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import static stinc.male.sandbox.ratexecutor.Preconditions.checkNotNull;

@Immutable
public final class RateMeterConfig {
  private static final RateMeterConfig defaultInstance = new Builder().build();

  private boolean checkArguments;
  private Function<Long, ? extends TicksCounter> ticksCounterSupplier;

  private RateMeterConfig(
      final boolean checkArguments,
      final Function<Long, ? extends TicksCounter> ticksCounterSupplier) {
    this.checkArguments = checkArguments;
    this.ticksCounterSupplier = ticksCounterSupplier;
  }

  /**
   * @return A reasonable configuration.
   */
  public static final RateMeterConfig defaultInstance() {
    return defaultInstance;
  }

  public static final Builder newBuilder() {
    return new Builder();
  }

  public final Builder toBuilder() {
    return new Builder(this);
  }

  /**
   * Checking of arguments of {@link RateMeter} methods requires some computations,
   * but is not practical in most cases. This configuration parameter specifies if
   * {@link AbstractRateMeter} MUST do this checking, or MUST omit it.
   * @return {@code false} by default.
   */
  public final boolean isCheckArguments() {
    return checkArguments;
  }

  /**
   * Specifies a supplier which MUST be used by {@link AbstractRateMeter} to create ticks counters.
   * Note that if {@link AbstractRateMeter} is used concurrently
   * then supplier MUST provide a thread-safe implementation of {@link TicksCounter}.
   * @return {@code AtomicLongTicksCounter::new} by default.
   */
  public final Function<Long, ? extends TicksCounter> getTicksCounterSupplier() {
    return ticksCounterSupplier;
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName()
        + "(checkArguments=" + checkArguments
        + ", ticksCounterSupplier=" + ticksCounterSupplier
        + ')';
  }

  @NotThreadSafe
  public static final class Builder {
    private boolean checkArguments;
    private Function<Long, ? extends TicksCounter> ticksCounterSupplier;

    private Builder() {
      checkArguments = false;
      ticksCounterSupplier = AtomicLongTicksCounter::new;
    }

    private Builder(final RateMeterConfig config) {
      checkArguments = config.checkArguments;
      ticksCounterSupplier = config.ticksCounterSupplier;
    }

    /**
     * @see RateMeterConfig#isCheckArguments()
     */
    public final Builder setCheckArguments(final boolean checkArguments) {
      this.checkArguments = checkArguments;
      return this;
    }

    /**
     * @param ticksCounterSupplier MUST be immutable.
     * @see RateMeterConfig#getTicksCounterSupplier()
     */
    public final Builder setTicksCounterSupplier(final Function<Long, ? extends TicksCounter> ticksCounterSupplier) {
      checkNotNull(ticksCounterSupplier, "ticksCounterSupplier");
      this.ticksCounterSupplier = ticksCounterSupplier;
      return this;
    }

    public final RateMeterConfig build() {
      return new RateMeterConfig(checkArguments, ticksCounterSupplier);
    }
  }
}