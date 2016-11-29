package stinc.male.sandbox.ratexecutor;

public final class ConcurrentRateMeterTest extends AbstractRateMeterTest {
  public ConcurrentRateMeterTest() {
    super(ConcurrentRateMeter::new);
  }
}