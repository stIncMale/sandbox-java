package stinc.male.sandbox.ratmex.meter;

public interface TicksCounter {
  void add(long delta);

  long get();
}