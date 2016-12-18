package stinc.male.sandbox.ratexecutor;

public final class ConcurrentSkipListMapRateMeterParallelTest extends AbstractRateMeterParallelTest {
  public ConcurrentSkipListMapRateMeterParallelTest() {
    super(ConcurrentSkipListMapRateMeter::new);
  }
}