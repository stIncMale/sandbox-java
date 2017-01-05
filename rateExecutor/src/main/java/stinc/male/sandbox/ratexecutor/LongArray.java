package stinc.male.sandbox.ratexecutor;

interface LongArray {
  int length();

  void set(int idx, long value);

  long get(int idx);

  void add(int idx, long delta);
}