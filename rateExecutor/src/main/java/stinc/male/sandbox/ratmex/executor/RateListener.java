package stinc.male.sandbox.ratmex.executor;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public interface RateListener<E> {//TODO stats for RateMeasuringExecutorService. only a sindle rate meter reading

  boolean onChange(Rate targetRate, E event) throws RateFailedException;
}