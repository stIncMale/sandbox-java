package stinc.male.sandbox.ratexecutor;

public final class ConcurrentSkipListMapRateMeterSerialTest extends AbstractRateMeterSerialTest {
  public ConcurrentSkipListMapRateMeterSerialTest() {
    super(ConcurrentSkipListMapRateMeter::new);
  }
}