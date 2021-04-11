package stincmale.sandbox.benchmarks.util;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import static java.lang.Boolean.parseBoolean;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import static org.openjdk.jmh.runner.options.TimeValue.milliseconds;

public final class JmhOptions {
  private static final boolean dryRun = parseBoolean(System.getProperty("sandbox.benchmark.dryRun", "false"));
  private static final boolean javaServer = !dryRun;
  private static final boolean javaEnableAssertions = dryRun;

  private JmhOptions() {
  }

  public static final OptionsBuilder newOptionsBuilder(@Nullable final Consumer<OptionsBuilder> forksWarmupIterationsTuner) {
    final OptionsBuilder result = new OptionsBuilder();
    result.jvmArgs(
        "-Xverify:all",
        "-Xms4096m",
        "-Xmx4096m",
        javaServer ? "-server" : "-client",
        javaEnableAssertions ? "-enableassertions" : "-disableassertions")
        .shouldDoGC(false)
        .syncIterations(true)
        .shouldFailOnError(true)
        .threads(1)
        .timeout(milliseconds(1000_000));
    if (dryRun) {
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

  public static final String includeBenchmarks(final Class<?> klass) {
    return klass.getName() + ".*";
  }

  public static final String includeBenchmarks(final Class<?> klass, final String benchmarkRegexp) {
    return klass.getName() + benchmarkRegexp;
  }

  public static final String[] jvmArgsDisableGc() {
    return new String[] {"-XX:+UnlockExperimentalVMOptions", "-XX:+UseEpsilonGC"};
  }

  public static final String[] jvmArgsDisableBiasedLocking() {
    return new String[] {"-XX:-UseBiasedLocking"};
  }

  /**
   * Example:
   * <pre>{@code
   *  new OptionsBuilder().jvmArgsAppend(concat(
   *    jvmArgsDisableGc(),
   *    jvmArgsDisableBiasedLocking()));
   * }</pre>
   */
  public static final String[] concat(final String[]... arrays) {
    return Stream.of(arrays)
        .flatMap(Arrays::stream)
        .toArray(String[]::new);
  }
}
