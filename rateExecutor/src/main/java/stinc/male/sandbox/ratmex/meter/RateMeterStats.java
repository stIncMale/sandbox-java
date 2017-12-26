package stinc.male.sandbox.ratmex.meter;

public interface RateMeterStats {
  boolean isEmpty();

  long failedAccuracyEventsCountForTick();

  long failedAccuracyEventsCountForRate();
}