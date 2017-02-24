package stinc.male.sandbox.ratexecutor;

import java.util.function.BooleanSupplier;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public interface WaitStrategy {
  void await(BooleanSupplier condition);
}