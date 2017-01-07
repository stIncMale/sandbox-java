package stinc.male.sandbox.ratexecutor;

public interface RateMeterStats {
  boolean isEmpty();

  long failedAccuracyEventsCountForTicksCount();

  long failedAccuracyEventsCountForTick();

  long failedAccuracyEventsCountForRate();
}