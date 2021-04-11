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
import static java.util.Arrays.stream;
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
import static stincmale.sandbox.benchmarks.util.JmhOptions.includeBenchmarks;
import static stincmale.sandbox.benchmarks.util.JmhOptions.newOptionsBuilder;

@TestInstance(Lifecycle.PER_CLASS)
public class ListPerformanceBenchmark {
  public ListPerformanceBenchmark() {
  }

  @Test
  public void run() throws RunnerException {
    new Runner(newOptionsBuilder(opts -> opts.forks(10)
        .warmupTime(milliseconds(100))
        .warmupIterations(12)
        .measurementTime(milliseconds(100))
        .measurementIterations(3))
        .include(includeBenchmarks(getClass()))
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
