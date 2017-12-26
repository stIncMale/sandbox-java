package stinc.male.sandbox.ratmex.meter;

abstract class AbstractTicksCounter implements TicksCounter {
  @Override
  public String toString() {
    return String.valueOf(get());
  }
}