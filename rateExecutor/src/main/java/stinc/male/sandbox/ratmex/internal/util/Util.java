package stinc.male.sandbox.ratmex.internal.util;

import java.util.Locale;

public final class Util {
  public static final String format(final String format, final Object... args) {
    return String.format(Locale.ROOT, format, args);
  }

  private Util() {
    throw new UnsupportedOperationException("The class isn't designed to be instantiated");
  }
}