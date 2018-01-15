package stinc.male.sandbox.ratmex.internal.util;

public final class Constants {
  /**
   * Set this compile time constant to true in order to remove all specially prepared {@code assert} statements from the bytecode.
   * A specially prepared {@code assert} statement is:<br>
   * {@code assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || booleanExpression}<br>
   * or<br>
   * {@code assert EXCLUDE_ASSERTIONS_FROM_BYTECODE || booleanExpression : stringExpression},<br>
   * where {@code booleanExpression} and {@code stringExpression} can be either compile time or run time expressions.
   * <p>
   * This trick works because of the documented behaviour of {@code javac}.
   * See <a href="https://stackoverflow.com/a/40919125">https://stackoverflow.com/a/40919125</a>
   * for the reasoning behind this approach.
   */
  public static final boolean EXCLUDE_ASSERTIONS_FROM_BYTECODE = false;

  private Constants() {
    throw new UnsupportedOperationException("The class isn't designed to be instantiated");
  }
}