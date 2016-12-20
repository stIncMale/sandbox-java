package stinc.male.sandbox.ratexecutor;

public interface TicksCounter {
  void add(long delta);

  long get();

  void reset();
}