package stinc.male.sandbox.ratexecutor;

import java.util.Locale;
import java.util.function.Supplier;

public final class Preconditions {
  private static final String format(final String format, final Object... args) {
    return String.format(Locale.ROOT, format, args);
  }

  public static final void checkNotNull(
      final Object argument,
      final String safeParamName) throws NullPointerException {
    if (argument == null) {
      throw new NullPointerException(format("The argument %s must not be null", safeParamName));
    }
  }

  public static final void checkArgument(
      final boolean checkArgumentExpression,
      final String safeParamName,
      final String safeParamRestrictionDescription) throws IllegalArgumentException {
    if (!checkArgumentExpression) {
      throw new IllegalArgumentException(
          format("The argument %s is illegal. %s", safeParamName, safeParamRestrictionDescription));
    }
  }

  public static final void checkArgument(
      final boolean checkArgumentExpression,
      final String safeParamName,
      final Supplier<String> safeParamRestrictionDescriptionSupplier) throws IllegalArgumentException {
    if (!checkArgumentExpression) {
      throw new IllegalArgumentException(
          format("The argument %s is illegal. %s", safeParamName, safeParamRestrictionDescriptionSupplier.get()));
    }
  }

  private Preconditions() {
    throw new UnsupportedOperationException("The class isn't designed to be instantiated");
  }
}