package stinc.male.sandbox.ratexecutor.tmp;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import stinc.male.sandbox.ratexecutor.ConcurrentRingBufferRateMeter;
import stinc.male.sandbox.ratexecutor.RateMeter;

public final class RatMeXTest {
  public RatMeXTest() {
  }

//  @Test
  public final void test() throws Exception {
    final Executor executor = Executors.newFixedThreadPool(10);
    final RateMeter rateMeter = new ConcurrentRingBufferRateMeter(System.nanoTime(), Duration.ofMillis(1));
    final RatMeX ratmex = new RatMeX(executor, rateMeter);
    ratmex.submit(() -> System.out.println(rateMeter.rate()), new ClosedInterval(10, 10));
    Thread.sleep(10_000);
  }
}