package stincmale.sandbox.benchmarks.util;

import static java.lang.Boolean.parseBoolean;
import java.util.ArrayList;
import java.util.Collection;
import javax.annotation.concurrent.NotThreadSafe;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import static org.openjdk.jmh.runner.options.TimeValue.milliseconds;

@NotThreadSafe
public final class JmhOptions {
  private static final boolean DRY_RUN = parseBoolean(System.getProperty("stincmale.sandbox.benchmarks.dryRun", "false"));
  private static final boolean JAVA_SERVER = true;
  private static final boolean JAVA_ENABLE_ASSERTIONS = DRY_RUN;
  private static final boolean JAVA_DISABLE_BIASED_LOCKING = false;
  private static final boolean JAVA_DISABLE_GC = false;

  private JmhOptions() {
  }

  public static final OptionsBuilder includingClass(final Class<?> klass) {
    final OptionsBuilder result = get();
    result.include(klass.getName() + ".*");
    return result;
  }

  public static final OptionsBuilder get() {
    final OptionsBuilder result = new OptionsBuilder();
    final Collection<String> jvmArgs = new ArrayList<>();
    jvmArgs.add("-Xfuture");
    jvmArgs.add("-Xshare:off");
    jvmArgs.add("-Xms2048m");
    jvmArgs.add("-Xmx2048m");
    jvmArgs.add(JAVA_SERVER ? "-server" : "-client");
    jvmArgs.add(JAVA_ENABLE_ASSERTIONS ? "-enableassertions" : "-disableassertions");
    if (JAVA_DISABLE_BIASED_LOCKING) {
      jvmArgs.add("-XX:-UseBiasedLocking");
    }
    if (JAVA_DISABLE_GC) {
      jvmArgs.add("-XX:+UnlockExperimentalVMOptions");
      jvmArgs.add("-XX:+UseEpsilonGC");
    }
    result.jvmArgs(jvmArgs.toArray(new String[0]))
        .shouldDoGC(false)
        .syncIterations(true)
        .shouldFailOnError(true)
        .threads(1)
        .timeout(milliseconds(5_000));
    if (DRY_RUN) {
      result.forks(1)
          .warmupTime(milliseconds(50))
          .warmupIterations(1)
          .measurementTime(milliseconds(50))
          .measurementIterations(1);
    } else {
      result.forks(4)
          .warmupTime(milliseconds(200))
          .warmupIterations(10)
          .measurementTime(milliseconds(200))
          .measurementIterations(20);
    }
    return result;
  }
}
