package stinc.male.sandbox.ratexecutor.tmp;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.junit.Test;
import stinc.male.sandbox.ratexecutor.ConcurrentRingBufferRateMeter;
import stinc.male.sandbox.ratexecutor.RateMeter;

public final class RatMeXTest {
  public RatMeXTest() {
  }

  @Test
  public final void test() throws Exception {
    final Executor executor = Executors.newFixedThreadPool(4);
    final RateMeter rateMeter = new ConcurrentRingBufferRateMeter(System.nanoTime(), Duration.ofMillis(1_000));
    final RatMeX ratmex = new RatMeX(executor, rateMeter);
    ratmex.submit(() -> {
//      if (System.currentTimeMillis() % 1000 == 0) System.out.println(rateMeter.rate());
      ;
    }, new ClosedInterval(1, 1));
    Thread.sleep(100_000);
    System.out.println(new ClosedInterval(10_000, 10_000));
    System.out.println(rateMeter.rateAverage());
  }
}