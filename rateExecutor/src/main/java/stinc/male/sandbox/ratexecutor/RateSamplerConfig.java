package stinc.male.sandbox.ratexecutor;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

@Immutable
public final class RateSamplerConfig {
  private static final RateSamplerConfig defaultInstance = new Builder().build();

  private boolean checkTNanos;

  private RateSamplerConfig(final boolean checkTNanos) {
    this.checkTNanos = checkTNanos;
  }

  /**
   * @return A reasonable configuration.
   */
  public static final RateSamplerConfig defaultInstance() {
    return defaultInstance;
  }

  public static final Builder newBuilder() {
    return new Builder();
  }

  public static final Builder newBuilder(final RateSamplerConfig config) {
    return new Builder(config);
  }

  /**
   * Specifies if {@link AbstractRateSampler} must check that tNanos is withing the allowed range.
   * @return {@code false} by default.
   */
  public final boolean isCheckTNanos() {
    return checkTNanos;
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName()
        + "(checkTNanos=" + checkTNanos
        + ')';
  }

  @NotThreadSafe
  public static final class Builder {
    private boolean checkTNanos;

    private Builder() {
      checkTNanos = false;
    }

    private Builder(final RateSamplerConfig config) {
      checkTNanos = config.checkTNanos;
    }

    /**
     * @see RateSamplerConfig#isCheckTNanos()
     */
    public final Builder setCheckTNanos(final boolean checkTNanos) {
      this.checkTNanos = checkTNanos;
      return this;
    }

    public final RateSamplerConfig build() {
      return new RateSamplerConfig(checkTNanos);
    }
  }
}