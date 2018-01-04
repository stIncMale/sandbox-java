package stinc.male.sandbox.ratmex.meter;

import java.util.function.BooleanSupplier;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public interface WaitStrategy {
  void await(BooleanSupplier condition);
}