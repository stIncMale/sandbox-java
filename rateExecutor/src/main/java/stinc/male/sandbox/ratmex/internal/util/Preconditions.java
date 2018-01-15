package stinc.male.sandbox.ratmex.internal.util;

import java.util.function.Supplier;
import javax.annotation.Nullable;
import static stinc.male.sandbox.ratmex.internal.util.Utils.format;

public final class Preconditions {
  public static final <T> T checkNotNull(
      @Nullable final T argument,
      final String safeParamName) throws NullPointerException {
    if (argument == null) {
      throw new NullPointerException(format("The argument %s must not be null", safeParamName));
    }
    return argument;
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