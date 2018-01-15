package stinc.male.sandbox.ratmex.internal.util;

import java.util.Locale;

public final class Utils {
  public static final String format(final String format, final Object... args) {
    return String.format(Locale.ROOT, format, args);
  }

  private Utils() {
    throw new UnsupportedOperationException("The class isn't designed to be instantiated");
  }
}