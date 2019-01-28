package stincmale.sandbox.benchmarks;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import static java.util.Arrays.stream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import static java.util.stream.IntStream.range;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import static org.openjdk.jmh.runner.options.TimeValue.milliseconds;
import static stincmale.sandbox.benchmarks.util.JmhOptions.includeClass;
import static stincmale.sandbox.benchmarks.util.JmhOptions.newOptionsBuilder;

/**
 * Test environment:
 * [single CPU] 3.4 GHz Intel Core i5 (4 cores),
 * [OS] macOS 10.13.6 (17G4015),
 * [JDK] OpenJDK 11.0.1+13 (<a href="https://jdk.java.net/11/">a build from Oracle</a>).
 * <pre>{@code
 * Benchmark                                              (listDescriptor)  (size)   Mode  Cnt      Score     Error   Units
 * ListPerformanceTest.createAndFillViaAdd                      ARRAY_LIST       5  thrpt   30  23450.844 ± 393.636  ops/ms
 * ListPerformanceTest.createAndFillViaAdd                      ARRAY_LIST      50  thrpt   30   2325.029 ± 175.646  ops/ms
 * ListPerformanceTest.createAndFillViaAdd                      ARRAY_LIST     500  thrpt   30    306.059 ±   1.061  ops/ms
 * ListPerformanceTest.createAndFillViaAdd                      ARRAY_LIST    5000  thrpt   30     44.977 ±   0.631  ops/ms
 * ListPerformanceTest.createAndFillViaAdd                      ARRAY_LIST   50000  thrpt   30      4.322 ±   0.058  ops/ms
 * ListPerformanceTest.createAndFillViaAdd      ARRAY_LIST_ENSURE_CAPACITY       5  thrpt   30  23056.374 ± 175.691  ops/ms
 * ListPerformanceTest.createAndFillViaAdd      ARRAY_LIST_ENSURE_CAPACITY      50  thrpt   30   2816.342 ± 159.828  ops/ms
 * ListPerformanceTest.createAndFillViaAdd      ARRAY_LIST_ENSURE_CAPACITY     500  thrpt   30    330.942 ±  15.854  ops/ms
 * ListPerformanceTest.createAndFillViaAdd      ARRAY_LIST_ENSURE_CAPACITY    5000  thrpt   30     61.201 ±   2.214  ops/ms
 * ListPerformanceTest.createAndFillViaAdd      ARRAY_LIST_ENSURE_CAPACITY   50000  thrpt   30      6.086 ±   0.111  ops/ms
 * ListPerformanceTest.createAndFillViaAdd                     LINKED_LIST       5  thrpt   30  27331.697 ± 416.481  ops/ms
 * ListPerformanceTest.createAndFillViaAdd                     LINKED_LIST      50  thrpt   30   3191.444 ±  43.744  ops/ms
 * ListPerformanceTest.createAndFillViaAdd                     LINKED_LIST     500  thrpt   30    338.698 ±   4.659  ops/ms
 * ListPerformanceTest.createAndFillViaAdd                     LINKED_LIST    5000  thrpt   30     37.574 ±   0.675  ops/ms
 * ListPerformanceTest.createAndFillViaAdd                     LINKED_LIST   50000  thrpt   30      3.668 ±   0.056  ops/ms
 * ListPerformanceTest.iterate                                  ARRAY_LIST       5  thrpt   30  58186.287 ± 198.915  ops/ms
 * ListPerformanceTest.iterate                                  ARRAY_LIST      50  thrpt   30  28137.938 ±  82.435  ops/ms
 * ListPerformanceTest.iterate                                  ARRAY_LIST     500  thrpt   30   4175.369 ±  46.936  ops/ms
 * ListPerformanceTest.iterate                                  ARRAY_LIST    5000  thrpt   30    449.363 ±   1.681  ops/ms
 * ListPerformanceTest.iterate                                  ARRAY_LIST   50000  thrpt   30     44.688 ±   0.907  ops/ms
 * ListPerformanceTest.iterate                                 LINKED_LIST       5  thrpt   30  59018.124 ± 235.894  ops/ms
 * ListPerformanceTest.iterate                                 LINKED_LIST      50  thrpt   30  13885.762 ±  41.791  ops/ms
 * ListPerformanceTest.iterate                                 LINKED_LIST     500  thrpt   30   1465.298 ±   5.509  ops/ms
 * ListPerformanceTest.iterate                                 LINKED_LIST    5000  thrpt   30     56.109 ±   0.205  ops/ms
 * ListPerformanceTest.iterate                                 LINKED_LIST   50000  thrpt   30      5.608 ±   0.023  ops/ms
 * }</pre>
 */
@TestInstance(Lifecycle.PER_CLASS)
public class ListPerformanceTest {
  public ListPerformanceTest() {
  }

  @Test
  public void run() throws RunnerException {
    new Runner(newOptionsBuilder(opts -> opts.forks(10)
        .warmupTime(milliseconds(100))
        .warmupIterations(12)
        .measurementTime(milliseconds(100))
        .measurementIterations(3))
        .include(includeClass(getClass()))
        .shouldDoGC(true)
        .mode(Mode.Throughput)
        .timeUnit(TimeUnit.MILLISECONDS)
        .build())
        .run();
  }

  @Benchmark
  public List<Object> createAndFillViaAdd(final ThreadStateForAdding state) {
    final int size = state.size;
    final List<Object> list = state.listDescriptor.create(size);
    for (int i = 0; i < size; i++) {
      list.add(new Object());
    }
    return list;
  }

  @Benchmark
  public boolean iterate(final ThreadStateForIterating state) {
    boolean result = false;
    final List<Object> list = state.listDescriptor.get(state.size);
    for (final Object o : list) {
      if (o == list) {
        result = true;
      }
    }
    return result;
  }

  @State(Scope.Thread)
  public static class ThreadStateForAdding {
    @Param({"5", "50", "500", "5000", "50000"})
    private int size;
    @Param({"ARRAY_LIST", "ARRAY_LIST_ENSURE_CAPACITY", "LINKED_LIST"})
    private ListDescriptor listDescriptor;

    public ThreadStateForAdding() {
    }

    public enum ListDescriptor {
      ARRAY_LIST(size -> new ArrayList<>()),
      ARRAY_LIST_ENSURE_CAPACITY(size -> {
        final ArrayList<Object> result = new ArrayList<>();
        result.ensureCapacity(size);
        return result;
      }),
      LINKED_LIST(size -> new LinkedList<>());

      private final Function<Integer, List<Object>> supplierOfNew;

      ListDescriptor(final Function<Integer, List<Object>> supplierOfNew) {
        this.supplierOfNew = supplierOfNew;
      }

      private List<Object> create(final int size) {
        return supplierOfNew.apply(size);
      }
    }
  }

  @State(Scope.Thread)
  public static class ThreadStateForIterating {
    private static final Map<Integer, ArrayList<Object>> arrayLists;
    private static final Map<Integer, LinkedList<Object>> linkedLists;

    static {
      final int[] sizes;
      try {
        sizes = stream(ThreadStateForAdding.class.getDeclaredField("size")
            .getAnnotation(Param.class)
            .value())
            .mapToInt(Integer::parseInt)
            .toArray();
      } catch (final NoSuchFieldException e) {
        throw new RuntimeException(e);
      }
      arrayLists = stream(sizes)
          .mapToObj(size -> new SimpleImmutableEntry<>(size, createList(size, ArrayList::new)))
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
      linkedLists = stream(sizes)
          .mapToObj(size -> new SimpleImmutableEntry<>(size, createList(size, LinkedList::new)))
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Param({"5", "50", "500", "5000", "50000"})
    private int size;
    @Param({"ARRAY_LIST", "LINKED_LIST"})
    private ListDescriptor listDescriptor;

    public ThreadStateForIterating() {
    }

    private static final <L extends List<Object>> L createList(final int size, final Supplier<L> listFactory) {
      return range(0, size)
          .mapToObj(i -> new Object())
          .collect(Collectors.toCollection(listFactory));
    }

    public enum ListDescriptor {
      ARRAY_LIST(arrayLists::get),
      LINKED_LIST(linkedLists::get);

      private final Function<Integer, List<Object>> supplierOfPreconstructed;

      ListDescriptor(final Function<Integer, List<Object>> supplierOfPreconstructed) {
        this.supplierOfPreconstructed = supplierOfPreconstructed;
      }

      private final List<Object> get(final int size) {
        return supplierOfPreconstructed.apply(size);
      }
    }
  }
}
