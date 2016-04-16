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
<<<<<<< d39cbd0aa262fc78a8a7734a038ead42ad99e74a
=======
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
>>>>>>> Create sandbox-rateExecutor project

/**
 * TODO
 */
@ThreadSafe
final class DefaultRateExecutor implements RateExecutor {
<<<<<<< d39cbd0aa262fc78a8a7734a038ead42ad99e74a
	private static final class ShotAction implements Runnable {
		private final Shot shot;
		private final RateAction rateAction;
=======
	private static final class Action implements Runnable {
		private final Runnable action;
		private final InternalActionCounter counter;

		private Action(final Runnable action, final InternalActionCounter actionCounter) {
			this.action = action;
			this.counter = actionCounter;
		}

		@Override
		public final void run() {
			try {
				action.run();
			} finally {
				counter.countCompleted();
			}
		}
	}

	private static final class ShotAction implements Runnable {
		private static final Logger logger = LoggerFactory.getLogger(ShotAction.class);

		private final Shot shot;
		private final Action action;
>>>>>>> Create sandbox-rateExecutor project
		private final ExecutorService executorService;
		private final DistributionStrategy distributionStrategy;
		private final ScheduledExecutorService starter;

		private ShotAction(
				final Shot shot,
<<<<<<< d39cbd0aa262fc78a8a7734a038ead42ad99e74a
				final RateAction rateAction,
=======
				final Action action,
>>>>>>> Create sandbox-rateExecutor project
				final ExecutorService executorService,
				final DistributionStrategy distributionStrategy,
				final ScheduledExecutorService starter) {
			this.shot = shot;
<<<<<<< d39cbd0aa262fc78a8a7734a038ead42ad99e74a
			this.rateAction = rateAction;
=======
			this.action = action;
>>>>>>> Create sandbox-rateExecutor project
			this.executorService = executorService;
			this.distributionStrategy = distributionStrategy;
			this.starter = starter;
		}

		@Override
		public final void run() {
<<<<<<< d39cbd0aa262fc78a8a7734a038ead42ad99e74a
			final long count = shot.getCount();
			for (long i = 0; i < count; i++) {
				executorService.execute(rateAction);
			}
			final Shot nexShot = distributionStrategy.getNext(rateAction);
			starter.schedule(new ShotAction(nexShot, rateAction, executorService, distributionStrategy, starter),
=======
			logger.trace("Firing {}", shot);
			for (long i = 0; i < shot.getCount(); i++) {
				action.counter.countStarted();
				executorService.submit(action);
			}
			final Shot nexShot = distributionStrategy.getNext(action.counter);
			logger.trace("Scheduling {}", nexShot);
			starter.schedule(new ShotAction(nexShot, action, executorService, distributionStrategy, starter),
>>>>>>> Create sandbox-rateExecutor project
					shot.getInstantNanos() - System.nanoTime(), TimeUnit.NANOSECONDS);
		}
	}

<<<<<<< d39cbd0aa262fc78a8a7734a038ead42ad99e74a
=======
	private static final Logger logger = LoggerFactory.getLogger(DefaultRateExecutor.class);
>>>>>>> Create sandbox-rateExecutor project
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
<<<<<<< d39cbd0aa262fc78a8a7734a038ead42ad99e74a
		internalExecute(new RateAction(action, rate, System.nanoTime()),
				rateActionConfig == null ? defaultRateAction : rateActionConfig);
=======
		internalExecute(action, rate, rateActionConfig == null ? defaultRateAction : rateActionConfig);
>>>>>>> Create sandbox-rateExecutor project
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

<<<<<<< d39cbd0aa262fc78a8a7734a038ead42ad99e74a
	private final void internalExecute(final RateAction rateAction, final RateActionConfig rateActionConfig) {
		final DistributionStrategy distributionStrategy = rateActionConfig.getDistributionStrategy();
		final Shot firstShot = distributionStrategy.getNext(rateAction);
		starter.schedule(
				new ShotAction(firstShot, rateAction, executorService, distributionStrategy, starter),
=======
	private final void internalExecute(final Runnable action, final Rate rate, final RateActionConfig rateActionConfig) {
		final DistributionStrategy distributionStrategy = rateActionConfig.getDistributionStrategy();
		final InternalActionCounter actionCounter = new InternalActionCounter(System.nanoTime(), rate);
		final Shot firstShot = distributionStrategy.getNext(actionCounter);
		logger.trace("Scheduling first {}", firstShot);
		starter.schedule(
				new ShotAction(firstShot, new Action(action, actionCounter), executorService, distributionStrategy, starter),
>>>>>>> Create sandbox-rateExecutor project
				firstShot.getInstantNanos() - System.nanoTime(), TimeUnit.NANOSECONDS);
	}
}
