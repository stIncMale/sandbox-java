package stinc.male.sandbox.ratmex.meter;

public interface RateMeterStats {
  long failedAccuracyEventsCountForTick();

  long failedAccuracyEventsCountForRate();
}