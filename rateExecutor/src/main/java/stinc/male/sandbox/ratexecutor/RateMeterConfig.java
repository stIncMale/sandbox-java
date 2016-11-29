package stinc.male.sandbox.ratexecutor;

import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;

@Immutable
public final class RateMeterConfig {
  private static final RateMeterConfig defaultInstance = new Builder().build();

  private boolean checkTNanos;

  private RateMeterConfig(final boolean checkTNanos) {
    this.checkTNanos = checkTNanos;
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
   * Specifies if {@link AbstractRateMeter} must check that tNanos is withing the allowed range.
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

    private Builder(final RateMeterConfig config) {
      checkTNanos = config.checkTNanos;
    }

    /**
     * @see RateMeterConfig#isCheckTNanos()
     */
    public final Builder setCheckTNanos(final boolean checkTNanos) {
      this.checkTNanos = checkTNanos;
      return this;
    }

    public final RateMeterConfig build() {
      return new RateMeterConfig(checkTNanos);
    }
  }
}