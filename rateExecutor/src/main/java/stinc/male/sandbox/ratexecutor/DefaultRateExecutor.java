package stinc.male.sandbox.ratexecutor;

import static stinc.male.sandbox.ratexecutor.Preconditions.checkNotNull;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * TODO
 */
@ThreadSafe
final class DefaultRateExecutor implements RateExecutor {
	private static final class ShotAction implements Runnable {
		private final Shot shot;
		private final RateAction rateAction;
		private final ExecutorService executorService;
		private final DistributionStrategy distributionStrategy;
		private final ScheduledExecutorService starter;

		private ShotAction(
				final Shot shot,
				final RateAction rateAction,
				final ExecutorService executorService,
				final DistributionStrategy distributionStrategy,
				final ScheduledExecutorService starter) {
			this.shot = shot;
			this.rateAction = rateAction;
			this.executorService = executorService;
			this.distributionStrategy = distributionStrategy;
			this.starter = starter;
		}

		@Override
		public final void run() {
			final long count = shot.getCount();
			for (long i = 0; i < count; i++) {
				executorService.execute(rateAction);
			}
			final Shot nexShot = distributionStrategy.getNext(rateAction);
			starter.schedule(new ShotAction(nexShot, rateAction, executorService, distributionStrategy, starter),
					shot.getInstantNanos() - System.nanoTime(), TimeUnit.NANOSECONDS);
		}
	}

	private static final RateActionConfig defaultRateAction;

	static {
		defaultRateAction = new RateActionConfig(new BurstDistributionStrategy());
	}

	private final ExecutorService executorService;
	private final ScheduledExecutorService starter;

	public DefaultRateExecutor(final ExecutorService executorService, final ThreadFactory threadFactory) {
		checkNotNull("first", executorService, "executorService");
		checkNotNull("second", threadFactory, "threadFactory");
		this.executorService = executorService;
		this.starter = Executors.newScheduledThreadPool(1, threadFactory);//TODO parametrize core pool size
	}

	@Override
	public final boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
		return executorService.awaitTermination(timeout, unit);
	}

	@Override
	public final void execute(final Runnable command) {
		executorService.execute(command);
	}

	@Override
	public final void execute(final Runnable action, final Rate rate) {
		checkNotNull("first", action, "action");
		checkNotNull("second", rate, "rate");
		execute(action, rate, null);
	}

	@Override
	public final void execute(final Callable<?> action, final Rate rate) {
		checkNotNull("first", action, "action");
		checkNotNull("second", rate, "rate");
		execute(action, rate, null);
	}

	@Override
	public final void execute(final Runnable action, final Rate rate, @Nullable final RateActionConfig rateActionConfig) {
		checkNotNull("first", action, "action");
		checkNotNull("second", rate, "rate");
		internalExecute(new RateAction(action, rate, System.nanoTime()),
				rateActionConfig == null ? defaultRateAction : rateActionConfig);
	}

	@Override
	public final void execute(final Callable<?> action, final Rate rate, @Nullable final RateActionConfig rateActionConfig) {
		checkNotNull("first", action, "action");
		checkNotNull("second", rate, "rate");
		execute(
				() -> {
					try {
						action.call();
					} catch (final Exception e) {
						throw new RuntimeException(e);
					}
				},
				rate, rateActionConfig);
	}

	@Override
	public final <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks) throws InterruptedException {
		return executorService.invokeAll(tasks);
	}

	@Override
	public final <T> List<Future<T>> invokeAll(
			final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit) throws InterruptedException {
		return executorService.invokeAll(tasks, timeout, unit);
	}

	@Override
	public final <T> T invokeAny(final Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
		return executorService.invokeAny(tasks);
	}

	@Override
	public final <T> T invokeAny(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		return executorService.invokeAny(tasks, timeout, unit);
	}

	@Override
	public final boolean isShutdown() {
		return executorService.isShutdown();
	}

	@Override
	public final boolean isTerminated() {
		return executorService.isTerminated();
	}

	@Override
	public final void shutdown() {
		try {
			executorService.shutdown();
		} finally {
			starter.shutdown();
		}
	}

	@Override
	public final List<Runnable> shutdownNow() {
		final List<Runnable> result;
		try {
			result = executorService.shutdownNow();
		} finally {
			starter.shutdownNow();
		}
		return result;
	}

	@Override
	public final <T> Future<T> submit(final Callable<T> task) {
		return executorService.submit(task);
	}

	@Override
	public final <T> Future<T> submit(final Runnable task, final T result) {
		return executorService.submit(task, result);
	}

	@Override
	public final Future<?> submit(final Runnable task) {
		return executorService.submit(task);
	}

	private final void internalExecute(final RateAction rateAction, final RateActionConfig rateActionConfig) {
		final DistributionStrategy distributionStrategy = rateActionConfig.getDistributionStrategy();
		final Shot firstShot = distributionStrategy.getNext(rateAction);
		starter.schedule(
				new ShotAction(firstShot, rateAction, executorService, distributionStrategy, starter),
				firstShot.getInstantNanos() - System.nanoTime(), TimeUnit.NANOSECONDS);
	}
}
