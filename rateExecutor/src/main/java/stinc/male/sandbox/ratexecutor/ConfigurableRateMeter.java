package stinc.male.sandbox.ratexecutor;

import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public interface ConfigurableRateMeter<C> extends RateMeter {
  C getConfig();
}