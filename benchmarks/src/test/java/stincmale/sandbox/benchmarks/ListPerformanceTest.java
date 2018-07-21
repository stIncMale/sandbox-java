package stincmale.sandbox.benchmarks;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
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
import stincmale.sandbox.benchmarks.util.JmhOptions;
import static java.util.Arrays.stream;
import static java.util.stream.IntStream.range;

/**
 * <pre>{@code
 * Benchmark                              (listDescriptor)  (size)   Mode  Cnt      Score     Error   Units
 * ListPerformanceTest.add                      ARRAY_LIST       5  thrpt   80  20324.444 ± 283.268  ops/ms
 * ListPerformanceTest.add                      ARRAY_LIST      50  thrpt   80   2306.665 ±  27.666  ops/ms
 * ListPerformanceTest.add                      ARRAY_LIST     500  thrpt   80    268.222 ±   0.986  ops/ms
 * ListPerformanceTest.add                      ARRAY_LIST    5000  thrpt   80     38.454 ±   0.933  ops/ms
 * ListPerformanceTest.add                      ARRAY_LIST   50000  thrpt   80      3.747 ±   0.081  ops/ms
 * ListPerformanceTest.add      ARRAY_LIST_ENSURE_CAPACITY       5  thrpt   80  20716.422 ±  78.477  ops/ms
 * ListPerformanceTest.add      ARRAY_LIST_ENSURE_CAPACITY      50  thrpt   80   2527.994 ±  16.440  ops/ms
 * ListPerformanceTest.add      ARRAY_LIST_ENSURE_CAPACITY     500  thrpt   80    262.597 ±   0.969  ops/ms
 * ListPerformanceTest.add      ARRAY_LIST_ENSURE_CAPACITY    5000  thrpt   80     55.183 ±   0.234  ops/ms
 * ListPerformanceTest.add      ARRAY_LIST_ENSURE_CAPACITY   50000  thrpt   80      5.306 ±   0.019  ops/ms
 * ListPerformanceTest.add                     LINKED_LIST       5  thrpt   80  24355.843 ± 113.264  ops/ms
 * ListPerformanceTest.add                     LINKED_LIST      50  thrpt   80   2830.576 ±  40.668  ops/ms
 * ListPerformanceTest.add                     LINKED_LIST     500  thrpt   80    295.308 ±   6.964  ops/ms
 * ListPerformanceTest.add                     LINKED_LIST    5000  thrpt   80     31.453 ±   0.669  ops/ms
 * ListPerformanceTest.add                     LINKED_LIST   50000  thrpt   80      3.136 ±   0.080  ops/ms
 * ListPerformanceTest.iterate                  ARRAY_LIST       5  thrpt   80  59849.843 ± 192.667  ops/ms
 * ListPerformanceTest.iterate                  ARRAY_LIST      50  thrpt   80  28736.416 ± 277.912  ops/ms
 * ListPerformanceTest.iterate                  ARRAY_LIST     500  thrpt   80   4479.838 ±  36.147  ops/ms
 * ListPerformanceTest.iterate                  ARRAY_LIST    5000  thrpt   80    459.256 ±   2.148  ops/ms
 * ListPerformanceTest.iterate                  ARRAY_LIST   50000  thrpt   80     45.611 ±   0.372  ops/ms
 * ListPerformanceTest.iterate                 LINKED_LIST       5  thrpt   80  60110.623 ± 802.743  ops/ms
 * ListPerformanceTest.iterate                 LINKED_LIST      50  thrpt   80  14248.574 ±  51.953  ops/ms
 * ListPerformanceTest.iterate                 LINKED_LIST     500  thrpt   80   1492.543 ±   7.587  ops/ms
 * ListPerformanceTest.iterate                 LINKED_LIST    5000  thrpt   80     55.632 ±   0.559  ops/ms
 * ListPerformanceTest.iterate                 LINKED_LIST   50000  thrpt   80      5.518 ±   0.055  ops/ms
 * }</pre>
 */
@TestInstance(Lifecycle.PER_CLASS)
public class ListPerformanceTest {
  public ListPerformanceTest() {
  }

  @Test
  public void run() throws RunnerException {
    new Runner(JmhOptions.includingClass(getClass())
        .mode(Mode.Throughput)
        .timeUnit(TimeUnit.MILLISECONDS)
        .shouldDoGC(true)
        .build())
        .run();
  }

  @Benchmark
  public List<Object> add(final ThreadStateForAdding state) {
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