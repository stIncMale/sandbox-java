package stinc.male.sandbox.ratexecutor;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public final class TestDefaultRateExecutor {
	public TestDefaultRateExecutor() {
	}

	@Test
	public final void test() throws InterruptedException {
		final RateExecutor rateExecutor
				= new DefaultRateExecutor(Executors.newFixedThreadPool(1), (runnable) -> new Thread(runnable));
		rateExecutor.execute(this::action, new Rate(1, 2, TimeUnit.SECONDS));
		TimeUnit.SECONDS.sleep(5);
	}

	private final void action() {
		System.out.println(Instant.now().getEpochSecond());
	}
}
