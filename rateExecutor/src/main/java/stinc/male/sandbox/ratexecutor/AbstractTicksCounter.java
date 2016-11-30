package stinc.male.sandbox.ratexecutor;

abstract class AbstractTicksCounter implements TicksCounter {
  @Override
  public String toString() {
    return String.valueOf(get());
  }
}