package stinc.male.sandbox.ratexecutor;

interface TicksCounter {
  long addAndGet(long delta);

  void add(long delta);

  long get();
}