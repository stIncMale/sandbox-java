package stinc.male.sandbox.ratmex.executor;

import javax.annotation.concurrent.NotThreadSafe;
import stinc.male.sandbox.ratmex.meter.RateMeterReading;

@NotThreadSafe
public interface RateListener {
  boolean onRate(Rate rate, RateMeterReading submitterReading, RateMeterReading workerReading) throws RateFailedException;
}