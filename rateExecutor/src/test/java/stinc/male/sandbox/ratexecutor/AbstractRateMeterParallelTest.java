package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static java.time.Duration.ofNanos;
import static org.junit.Assert.assertEquals;

public abstract class AbstractRateMeterParallelTest extends AbstractRateMeterTest {
  private final Function<Long, ? extends TicksCounter> ticksCounterSupplier;
  private final int numberOfThreads;
  private ExecutorService ex;

  AbstractRateMeterParallelTest(
      final RateMeterCreator rateMeterCreator,
      final Function<Long, ? extends TicksCounter> ticksCounterSupplier,
      final int numberOfThreads) {
    super(rateMeterCreator);
    this.ticksCounterSupplier = ticksCounterSupplier;
    this.numberOfThreads = numberOfThreads;
  }

  @Test
  public final void test() throws InterruptedException {
    final ThreadLocalRandom rnd = ThreadLocalRandom.current();
    for (int i = 1; i <= 1_500; i++) {
      final Duration samplesInterval = ofNanos(rnd.nextInt(1, 1_000));
      final TestParams tp = new TestParams(
          numberOfThreads,
          1_500,
          rnd.nextBoolean(),
          rnd.nextInt(0, 5),
          samplesInterval,
          rnd.nextBoolean());
      doTest(i, tp, ex);
    }
  }

  @Before
  public final void before() {
    ex = Executors.newFixedThreadPool(numberOfThreads);
  }

  @After
  public final void after() {
    ex.shutdownNow();
  }

  private final void doTest(final int iterationIdx, final TestParams tp, final ExecutorService ex) {
    final ThreadLocalRandom rnd = ThreadLocalRandom.current();
    final long startNanos = rnd.nextLong();
    final RateMeter rm = getRateMeterCreator().create(
        startNanos,
        tp.samplesInterval,
        RateMeterConfig.newBuilder()
            .setCheckArguments(true)
            .setTimeSensitivity(ofNanos(1))
            .setTicksCounterSupplier(ticksCounterSupplier)
            .build());
    final TickGenerator tickGenerator = new TickGenerator(
        startNanos,
        tp.repeatingInstants
            ? startNanos + tp.samplesInterval.toNanos()//with repeating instants one can not predict the result unless all samples happen to be in the same samples window
            : startNanos + (long) (rnd.nextDouble(0.001, 1000) * tp.samplesInterval.toNanos()),
        tp.repeatingInstants,
        tp.numberOfSamples,
        tp.numberOfThreads);
    final Collection<TickGenerator> tickGenerators = tickGenerator.split();
    tickGenerators.stream()
        .map(ticksGenerator -> ticksGenerator.generate(rm, tp.orderTicksByTime, tp.tickToRateRatio, ex))
        .collect(Collectors.toList())
        .forEach(futureGenerate -> {
          try {
            futureGenerate.get();
          } catch (final Exception e) {
            throw new RuntimeException(e);
          }
        });
    assertEquals(String.format("Iteration#%s, %s", iterationIdx, tp), tickGenerator.rightmostTNanos(), rm.rightSamplesWindowBoundary());
    assertEquals(String.format("Iteration#%s, %s", iterationIdx, tp), tickGenerator.countRightmost(tp.samplesInterval.toNanos()), rm.ticksCount());
    assertEquals(String.format("Iteration#%s, %s", iterationIdx, tp), tickGenerator.totalCount(), rm.ticksTotalCount());
  }

  private static final class TestParams {
    final int numberOfThreads;
    final int numberOfSamples;
    final boolean orderTicksByTime;
    final int tickToRateRatio;
    final Duration samplesInterval;
    final boolean repeatingInstants;

    TestParams(
        final int numberOfThreads,
        final int numberOfSamples,
        final boolean orderTicksByTime,
        final int tickToRateRatio,
        final Duration samplesInterval,
        final boolean repeatingInstants) {
      this.numberOfThreads = numberOfThreads;
      this.numberOfSamples = numberOfSamples;
      this.orderTicksByTime = orderTicksByTime;
      this.tickToRateRatio = tickToRateRatio;
      this.samplesInterval = samplesInterval;
      this.repeatingInstants = repeatingInstants;
    }

    @Override
    public final String toString() {
      return getClass().getSimpleName()
          + "(numberOfThreads=" + numberOfThreads
          + ", numberOfSamples=" + numberOfSamples
          + ", orderTicksByTime=" + orderTicksByTime
          + ", tickToRateRatio=" + tickToRateRatio
          + ", samplesInterval=" + samplesInterval
          + ", repeatingInstants=" + repeatingInstants
          + ')';
    }
  }

  private final class TickGenerator {
    private final NavigableMap<Long, Long> samples;
    private final boolean repeatingInstants;
    private final int splitN;

    TickGenerator(
        final long startNanos,
        final long maxTNanosInclusive,
        final boolean repeatingInstants,
        final int numberOfSamples,
        final int splitN) {
      final ThreadLocalRandom rnd = ThreadLocalRandom.current();
      samples = new TreeMap<>(NanosComparator.instance());
      samples.put(startNanos, 0L);
      for (int i = 0; i < numberOfSamples - 1; i++) {
        final long tNanos = rnd.nextLong(startNanos, maxTNanosInclusive + 1);
        final long count = rnd.nextLong(-3, 4);
        samples.put(tNanos, count);
      }
      this.repeatingInstants = repeatingInstants;
      this.splitN = splitN;
    }

    TickGenerator(final NavigableMap<Long, Long> samples) {
      this.samples = samples;
      repeatingInstants = false;
      splitN = -1;
    }

    final Future<?> generate(final RateMeter rm, boolean orderTicksByTime, int tickToRateRatio, final ExecutorService ex) {
      final List<Entry<Long, Long>> shuffledSamples = new ArrayList<>(samples.entrySet());
      if (!orderTicksByTime) {
        Collections.shuffle(shuffledSamples);
      }
      final Future<?> result = ex.submit(() -> {
        int i = 0;
        shuffledSamples.forEach(sample -> {
          rm.tick(sample.getValue(), sample.getKey());
          if (tickToRateRatio > 0 && i % tickToRateRatio == 0) {
            if (ThreadLocalRandom.current().nextBoolean()) {
              rm.rate();
            } else {
              rm.rate(rm.rightSamplesWindowBoundary());
            }
          }
        });
      });
      return result;
    }

    final Collection<TickGenerator> split() {
      final List<NavigableMap<Long, Long>> splitSamples = new ArrayList<>(splitN);
      if (repeatingInstants) {
        for (int i = 0; i < splitN; i++) {
          splitSamples.add(new TreeMap<>(samples));
        }
      } else {
        for (int i = 0; i < splitN; i++) {
          splitSamples.add(new TreeMap<>(NanosComparator.instance()));
        }
        final List<Entry<Long, Long>> listOfSamples = new ArrayList<>(samples.entrySet());
        for (int i = 0; i < listOfSamples.size(); i++) {
          final Entry<Long, Long> sample = listOfSamples.get(i);
          splitSamples.get(i % splitSamples.size())
              .put(sample.getKey(), sample.getValue());
        }
      }
      return splitSamples.stream()
          .map(splitSample -> new TickGenerator(splitSample))
          .collect(Collectors.toList());
    }

    final long countRightmost(final long samplesIntervalNanos) {
      final long rightNanos = rightmostTNanos();
      final long leftNanos = rightNanos - samplesIntervalNanos;
      return samples.subMap(leftNanos, false, rightNanos, true).values()
          .stream()
          .mapToLong(Long::longValue)
          .sum() * (repeatingInstants ? splitN : 1);
    }

    final long totalCount() {
      return samples.values()
          .stream()
          .mapToLong(Long::longValue)
          .sum() * (repeatingInstants ? splitN : 1);
    }

    final long rightmostTNanos() {
      final long leftmost = samples.firstKey();
      long result = samples.lastKey();
      while (samples.get(result) == 0 && result != leftmost) {
        result = samples.lowerKey(result);
      }
      return result;
    }
  }
}