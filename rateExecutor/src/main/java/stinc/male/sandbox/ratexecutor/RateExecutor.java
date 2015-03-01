package stinc.male.sandbox.ratexecutor;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * TODO
 */
@ThreadSafe
public interface RateExecutor extends ExecutorService {
	void execute(Runnable action, Rate rate);

	void execute(Callable<?> action, Rate rate);

	void execute(Runnable action, Rate rate, @Nullable RateActionConfig rateConfig);

	void execute(Callable<?> action, Rate rate, @Nullable RateActionConfig rateConfig);
}
