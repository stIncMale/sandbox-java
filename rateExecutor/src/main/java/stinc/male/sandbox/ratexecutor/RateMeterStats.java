package stinc.male.sandbox.ratexecutor;

public interface RateMeterStats {
  boolean isEmpty();

  long failedAccuracyEventsCountForTick();

  long failedAccuracyEventsCountForRate();
}