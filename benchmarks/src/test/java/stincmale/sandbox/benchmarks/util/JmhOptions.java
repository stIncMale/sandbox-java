package stincmale.sandbox.benchmarks.util;

import static java.lang.Boolean.parseBoolean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import static org.openjdk.jmh.runner.options.TimeValue.milliseconds;

public final class JmhOptions {
  private static final boolean DRY_RUN = parseBoolean(System.getProperty("stincmale.sandbox.benchmarks.dryRun", "false"));
  private static final boolean JAVA_SERVER = true;
  private static final boolean JAVA_ENABLE_ASSERTIONS = DRY_RUN;

  private JmhOptions() {
  }

  public static final OptionsBuilder newOptionsBuilder(@Nullable final Consumer<OptionsBuilder> forksWarmupIterationsTuner) {
    final OptionsBuilder result = new OptionsBuilder();
    final Collection<String> jvmArgs = new ArrayList<>();
    jvmArgs.add("-Xfuture");
    jvmArgs.add("-Xshare:off");
    jvmArgs.add("-Xms4096m");
    jvmArgs.add("-Xmx4096m");
    jvmArgs.add(JAVA_SERVER ? "-server" : "-client");
    jvmArgs.add(JAVA_ENABLE_ASSERTIONS ? "-enableassertions" : "-disableassertions");
    result.jvmArgs(jvmArgs.toArray(new String[0]))
        .shouldDoGC(false)
        .syncIterations(true)
        .shouldFailOnError(true)
        .threads(1)
        .timeout(milliseconds(1000_000));
    if (DRY_RUN) {
      result.forks(1)
          .warmupTime(milliseconds(50))
          .warmupIterations(1)
          .measurementTime(milliseconds(50))
          .measurementIterations(1);
    } else if (forksWarmupIterationsTuner != null) {
      forksWarmupIterationsTuner.accept(result);
    }
    return result;
  }

  public static final String includeClass(final Class<?> klass) {
    return klass.getName() + ".*";
  }

  public static final String[] jvmArgsDisableGc() {
    return new String[] {"-XX:+UnlockExperimentalVMOptions", "-XX:+UseEpsilonGC"};
  }

  public static final String[] jvmArgsDisableBiasedLocking() {
    return new String[] {"-XX:-UseBiasedLocking"};
  }

  public static final String[] concat(final String[]... arrays) {
    return Stream.of(arrays)
        .flatMap(Arrays::stream)
        .toArray(String[]::new);
  }
}
