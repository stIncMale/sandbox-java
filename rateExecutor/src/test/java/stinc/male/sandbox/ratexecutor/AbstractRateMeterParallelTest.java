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
import java.util.stream.Collectors;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public abstract class AbstractRateMeterParallelTest extends AbstractRateMeterTest {
  AbstractRateMeterParallelTest(final RateMeterCreator rateMeterCreator) {
    super(rateMeterCreator);
  }

  @Test
  public final void test() throws InterruptedException {
    final ThreadLocalRandom rnd = ThreadLocalRandom.current();
    for (int i = 1; i <= 1_000; i++) {
      final Duration samplesInterval = Duration.ofMillis(rnd.nextInt(1_000));
      final TestParams tp = new TestParams(
              10,
              10_000,
              rnd.nextBoolean(),
              rnd.nextInt(0, 10),
              Duration.ofMillis(rnd.nextInt(1, 1_000)),
              true
                      ? Duration.ofNanos(1)
                      : Duration.ofNanos((long)(ThreadLocalRandom.current().nextDouble(0.001, 0.5) * samplesInterval.toNanos())));
      doTest(i, tp);
    }
  }

  private final void doTest(final int iterationIdx, final TestParams tp) {
    final long startNanos = ThreadLocalRandom.current().nextLong();
    final RateMeter rm = getRateMeterCreator().create(
            startNanos,
            tp.samplesInterval,
            RateMeterConfig.newBuilder()
                    .setCheckArguments(true)
                    .setTimeSensitivity(tp.timeSensitivity)
                    .build());
    final TickGenerator tickGenerator = new TickGenerator(startNanos, tp.samplesInterval, tp.numberOfSamples);
    final Collection<TickGenerator> tickGenerators = tickGenerator.split(tp.numberOfThreads);
    tickGenerators.stream()
        .map(ticksGenerator -> ticksGenerator.sink(rm, tp.orderTicksByTime, tp.tickToRateRatio))
        .collect(Collectors.toList())
        .forEach(futureSink -> {
          try {
            futureSink.get();
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
    final Duration timeSensitivity;

    TestParams(
        final int numberOfThreads,
        final int numberOfSamples,
        final boolean orderTicksByTime,
        final int tickToRateRatio,
        final Duration samplesInterval,
        final Duration timeSensitivity) {
      this.numberOfThreads = numberOfThreads;
      this.numberOfSamples = numberOfSamples;
      this.orderTicksByTime = orderTicksByTime;
      this.tickToRateRatio = tickToRateRatio;
      this.samplesInterval = samplesInterval;
      this.timeSensitivity = timeSensitivity;
    }

    @Override
    public final String toString() {
      return getClass().getSimpleName()
          + "(numberOfThreads=" + numberOfThreads
          + ", numberOfSamples=" + numberOfSamples
          + ", orderTicksByTime=" + orderTicksByTime
          + ", tickToRateRatio=" + tickToRateRatio
          + ", samplesInterval=" + samplesInterval
          + ", timeSensitivity=" + timeSensitivity
          + ')';
    }
  }

  private final class TickGenerator {
    NavigableMap<Long, Long> samples;

    TickGenerator(final long startNanos, final Duration samplesInterval, final int numberOfSamples) {
      final ThreadLocalRandom rnd = ThreadLocalRandom.current();
      final long maxTNanos = startNanos + (long)(rnd.nextDouble(0.001, 1000) * samplesInterval.toNanos());
      samples = new TreeMap<>(NanosComparator.instance());
      samples.put(startNanos, 0L);
      for (int i = 0; i < numberOfSamples - 1; i++) {
        samples.put(
            rnd.nextLong(startNanos, maxTNanos),
            rnd.nextLong(-3, 4)
        );
      }
    }

    TickGenerator(final NavigableMap<Long, Long> samples) {
      this.samples = samples;
    }

    final Future<?> sink(final RateMeter rm, boolean orderTicksByTime, int tickToRateRatio) {//TODO use ticks with the same tNanos from different threads
      final List<Entry<Long, Long>> shuffledSamples = new ArrayList<>(samples.entrySet());
      if (!orderTicksByTime) {
        Collections.shuffle(shuffledSamples);
      }
      final Future<?> result;
      final ExecutorService ex = Executors.newSingleThreadExecutor();
      try {
        result = ex.submit(() -> {
          int i = 0;
          shuffledSamples.forEach(sample -> {
            rm.tick(sample.getValue(), sample.getKey());
            if (tickToRateRatio > 0 && i % tickToRateRatio == 0) {
              rm.rate();
            }
          });
        });
      } finally {
        ex.shutdownNow();
      }
      return result;
    }

    final Collection<TickGenerator> split(final int n) {
      final List<NavigableMap<Long, Long>> maps = new ArrayList<>(n);
      for (int i = 0; i < n; i++) {
        maps.add(new TreeMap<>(NanosComparator.instance()));
      }
      final List<Entry<Long, Long>> listOfSamples = new ArrayList<>(samples.entrySet());
      for (int i = 0; i < listOfSamples.size(); i++) {
        final Entry<Long, Long> sample = listOfSamples.get(i);
        maps.get(i % maps.size())
            .put(sample.getKey(), sample.getValue());
      }
      return maps.stream()
          .map(TickGenerator::new)
          .collect(Collectors.toList());
    }

    final long countRightmost(final long samplesIntervalNanos) {
      final long rightNanos = rightmostTNanos();
      final long leftNanos = rightNanos - samplesIntervalNanos;
      return samples.subMap(leftNanos, false, rightNanos, true).values()
              .stream()
              .mapToLong(Long::longValue)
              .sum();
    }

    final long totalCount() {
      return samples.values()
              .stream()
              .mapToLong(Long::longValue)
              .sum();
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