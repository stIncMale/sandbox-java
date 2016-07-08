package stinc.male.sandbox.ratexecutor;

public final class ConcurrentRateSamplerTest extends AbstractRateSamplerTest {
  public ConcurrentRateSamplerTest() {
    super(ConcurrentRateSampler::new);
  }
}