package stinc.male.sandbox.ratexecutor.tmp;

import javax.annotation.Nullable;

public class RateStrategyException extends RuntimeException {
  private static final long serialVersionUID = 0;

  public RateStrategyException() {
  }

  /**
   * @param message
   * The detail message which is saved for later retrieval by the {@link #getMessage()} method.
   */
  public RateStrategyException(@Nullable final String message) {
    super(message);
  }

  /**
   * @param message
   * The detail message which is saved for later retrieval by the {@link #getMessage()} method.
   * @param cause
   * The cause which is saved for later retrieval by the {@link #getCause()} method.
   */
  public RateStrategyException(@Nullable final String message, @Nullable final Throwable cause) {
    super(message, cause);
  }

  /**
   * @param cause
   * The cause which is saved for later retrieval by the {@link #getCause()} method.
   */
  public RateStrategyException(@Nullable final Throwable cause) {
    super(cause);
  }
}