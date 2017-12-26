package stinc.male.sandbox.ratmex.meter;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public interface ConfigurableRateMeter<C> extends RateMeter {
  C getConfig();
}