package stinc.male.sandbox.ratexecutor;

<<<<<<< d39cbd0aa262fc78a8a7734a038ead42ad99e74a
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
=======
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.ThreadSafe;
import org.junit.Test;

public final class TestDefaultRateExecutor {
	private final AtomicLong counter;

	public TestDefaultRateExecutor() {
		counter = new AtomicLong();
	}

//	@Test
//	public final void test() throws InterruptedException {
//		final RateExecutor rateExecutor
//				= new DefaultRateExecutor(Executors.newFixedThreadPool(1), (runnable) -> new Thread(runnable));
//		rateExecutor.execute(this::action, new Rate(1, 2, TimeUnit.SECONDS));
//		TimeUnit.SECONDS.sleep(10);
//		rateExecutor.shutdownNow();
//	}

	private final void action() {
		System.out.println("TODO nanos=" + System.nanoTime() + ", #" + counter.getAndIncrement());
	}

	@Test
	public final void test() {
		FibGen gen = new DumbFibGen();
		for (int i = 0; i < 100; i++) {
			System.out.println(gen.next());
		}
	}
}

@ThreadSafe
interface FibGen {
	BigInteger next();
}

final class DumbFibGen implements FibGen {
	@Immutable
	private static class State {
		private final long idx;
		private final BigInteger previous;
		private final BigInteger current;

		private State(long idx, BigInteger previous, BigInteger current) {
			this.idx = idx;
			this.previous = previous;
			this.current = current;
		}
	}

	private final AtomicReference<State> stateRef;

	public DumbFibGen() {
		stateRef = new AtomicReference<>(new State(0, BigInteger.ZERO, BigInteger.ONE));
	}

	@Override
	public BigInteger next() {
		State state;
		State newState;
		BigInteger result;
		do {
			state = stateRef.get();
			if (state.idx > 1) {
				result = state.previous.add(state.current);
				newState = new State(state.idx + 1, state.current, result);
			} else {
				if (state.idx == 1) {
					result = state.current;
				} else { //state.idx == 0
					result = state.previous;
				}
				newState = new State(state.idx + 1, state.previous, state.current);
			}
		} while (!stateRef.compareAndSet(state, newState));
		return result;
>>>>>>> Create sandbox-rateExecutor project
	}
}
