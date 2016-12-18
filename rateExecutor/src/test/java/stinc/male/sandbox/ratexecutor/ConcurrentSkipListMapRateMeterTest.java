package stinc.male.sandbox.ratexecutor;

public final class ConcurrentSkipListMapRateMeterTest extends AbstractRateMeterSerialTest {
  public ConcurrentSkipListMapRateMeterTest() {
    super(ConcurrentSkipListMapRateMeter::new);
  }
}