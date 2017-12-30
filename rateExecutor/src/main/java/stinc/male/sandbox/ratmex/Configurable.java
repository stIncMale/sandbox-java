package stinc.male.sandbox.ratmex;

/**
 * Any object which can be configured and which exposes its configuration.
 *
 * @param <C> A type of the {@linkplain #getConfig() configuration}.
 */
public interface Configurable<C> {
  /**
   * @return A configuration of the object.
   */
  C getConfig();
}