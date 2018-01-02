package stinc.male.sandbox.ratmex.meter;

import java.util.function.BooleanSupplier;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public interface WaitStrategy {//TODO use Thread.onSpinWait()
  void await(BooleanSupplier condition);//TODO add backoff (change the signature)
}