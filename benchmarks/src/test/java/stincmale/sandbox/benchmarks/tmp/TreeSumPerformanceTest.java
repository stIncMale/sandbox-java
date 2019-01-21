package stincmale.sandbox.benchmarks.tmp;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import static org.openjdk.jmh.runner.options.TimeValue.milliseconds;
import static stincmale.sandbox.benchmarks.util.JmhOptions.includeClass;
import static stincmale.sandbox.benchmarks.util.JmhOptions.jvmArgsDisableGc;
import static stincmale.sandbox.benchmarks.util.JmhOptions.newOptionsBuilder;

/**
 * Test environment:
 * [single CPU] 3.4 GHz Intel Core i5 (4 cores),
 * [OS] macOS 10.13.6 (17G4015),
 * [JDK] OpenJDK 11.0.1+13 (<a href="https://jdk.java.net/11/">a build from Oracle</a>).
 * <pre>{@code
 * Benchmark                               Mode  Cnt    Score   Error   Units
 * RemainderPerformanceTest.sum           thrpt   30   33.883 ± 0.115  ops/ms
 * RemainderPerformanceTest.sumWithCache  thrpt   30  137.265 ± 5.024  ops/ms
 * }</pre>
 */
@TestInstance(Lifecycle.PER_CLASS)
public class TreeSumPerformanceTest {
  public TreeSumPerformanceTest() {
  }

  @Test
  public void run() throws RunnerException {
    new Runner(newOptionsBuilder(opts -> opts.forks(10)
        .warmupTime(milliseconds(100))
        .warmupIterations(10)
        .measurementTime(milliseconds(100))
        .measurementIterations(3))
        .include(includeClass(getClass()))
        .jvmArgsAppend(jvmArgsDisableGc())
        .mode(Mode.Throughput)
        .timeUnit(TimeUnit.MILLISECONDS)
        .build())
        .run();
  }

  @Benchmark
  public int sum(final ThreadState state) {
    int preventDeadCodeElimination = 0;
    for (int i = 0; i < state.nodes.length; i++) {
      preventDeadCodeElimination += sum(state.nodes[i]);
    }
    return preventDeadCodeElimination;
  }

  @Benchmark
  public int sumWithCache(final ThreadState state) {
    int preventDeadCodeElimination = 0;
    for (int i = 0; i < state.nodes.length; i++) {
      preventDeadCodeElimination += sum(state.nodes[i], state.cache);
    }
    return preventDeadCodeElimination;
  }

  @State(Scope.Thread)
  public static class ThreadState {
    private Node[] nodes;
    private Map<Node, Integer> cache;

    public ThreadState() {
    }

    @Setup(Level.Iteration)
    public final void setup() {
      final Random rnd = new Random(0);
      nodes = new Node[1234];
      nodes[0] = new Node(rnd.nextInt());
      for (int parentIdx = 0, childIdx = 1; childIdx < nodes.length - 1; parentIdx++) {
        final Node node = nodes[parentIdx];
        node.left = nodes[childIdx++] = new Node(rnd.nextInt());
        if (childIdx == nodes.length - 1) {
          break;
        }
        node.right = nodes[childIdx++] = new Node(rnd.nextInt());
      }
      cache = new HashMap<>();
    }
  }

  private static final class Node {
    @Nullable
    private Node left;
    @Nullable
    private Node right;
    private final int value;

    private Node(final int value) {
      this.value = value;
    }
  }

  private static final int sum(@Nullable final Node node) {
    if (node == null) {
      return 0;
    }
    return node.value + sum(node.left) + sum(node.right);
  }

  private static final int sum(@Nullable final Node node, final Map<Node, Integer> cache) {
    if (node == null) {
      return 0;
    }
    @Nullable Integer cachedResult = cache.get(node);
    if (cachedResult != null) {
      return cachedResult;
    }
    cachedResult = node.value + sum(node.left, cache) + sum(node.right, cache);
    cache.put(node, cachedResult);
    return cachedResult;
  }
}
