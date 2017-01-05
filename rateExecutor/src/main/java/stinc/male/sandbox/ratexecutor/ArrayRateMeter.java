package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public class ArrayRateMeter extends AbstractRateMeter {
  private static final RateMeterConfig defaultConfigInstance = RateMeterConfig.newBuilder()
      .setTicksCounterSupplier(LongTicksCounter::new)
      .build();

  private final long[] samples;//length is even
  private final long samplesWindowStepNanos;
  private long samplesWindowShiftSteps;

  /**
   * @return A reasonable configuration.
   */
  public static final RateMeterConfig defaultConfig() {
    return defaultConfigInstance;
  }

  /**
   * @param startNanos Starting point that is used to calculate elapsed nanoseconds.
   * @param samplesInterval Size of the samples window.
   * @param config Additional configuration parameters.
   */
  public ArrayRateMeter(
      final long startNanos,
      final Duration samplesInterval,
      final RateMeterConfig config) {
    super(startNanos, samplesInterval, config);
    final long sensitivityNanos = config.getTimeSensitivity().toNanos();
    final long samplesIntervalNanos = getSamplesIntervalNanos();
    final int samplesIntervalArrayLength = (int)(samplesIntervalNanos / sensitivityNanos);
    Preconditions.checkArgument(samplesIntervalNanos / samplesIntervalArrayLength * samplesIntervalArrayLength == samplesIntervalNanos, "samplesInterval",
        () -> String.format(
            "The specified samplesInterval %snanos and timeSensitivity %snanos can not be used together because samplesInterval can not be devided evenly by timeSensitivity",
            samplesIntervalArrayLength, sensitivityNanos));
    samplesWindowStepNanos = samplesIntervalNanos / samplesIntervalArrayLength;
    samples = new long[2 * samplesIntervalArrayLength];
    samplesWindowShiftSteps = 0;
  }

  /**
   * Acts like {@link #ArrayRateMeter(long, Duration, RateMeterConfig)} with {@link #defaultConfig()}
   * as the third argument.
   */
  public ArrayRateMeter(final long startNanos, final Duration samplesInterval) {
    this(startNanos, samplesInterval, defaultConfig());
  }

  @Override
  public long rightSamplesWindowBoundary() {
    return rightSamplesWindowBoundary(samplesWindowShiftSteps);
  }

  @Override
  public long ticksCount() {
    final long samplesWindowShiftSteps = this.samplesWindowShiftSteps;
    long result = 0;
    for (int idx = leftSamplesWindowIdx(samplesWindowShiftSteps), i = 0;
         i < samples.length / 2;
         idx = nextSamplesWindowIdx(idx), i++) {
      result += samples[idx];
    }
    return result;
  }

  @Override
  public void tick(final long count, final long tNanos) {
    checkArgument(tNanos, "tNanos");
    if (count != 0) {
      final long targetSamplesWindowShiftSteps = samplesWindowShiftSteps(tNanos);
      final long samplesWindowShiftSteps = this.samplesWindowShiftSteps;
      if (samplesWindowShiftSteps - samples.length < targetSamplesWindowShiftSteps) {//tNanos is within the samples history
        if (samplesWindowShiftSteps < targetSamplesWindowShiftSteps) {//we need to move the samples window
          this.samplesWindowShiftSteps = targetSamplesWindowShiftSteps;
          final long numberOfStepsToMove = targetSamplesWindowShiftSteps - samplesWindowShiftSteps;
          final long numberOfIterations = Math.min(samples.length, numberOfStepsToMove);
          for (int idx = nextSamplesWindowIdx(rightSamplesWindowIdx(samplesWindowShiftSteps)), i = 0;
               i < numberOfIterations;
               idx = nextSamplesWindowIdx(idx), i++) {//reset moved samples
            samples[idx] = 0;
          }
        }
        samples[rightSamplesWindowIdx(targetSamplesWindowShiftSteps)] += count;
      }
      getTicksTotalCounter().add(count);
    }
  }

  @Override
  public double rateAverage(final long tNanos) {
    checkArgument(tNanos, "tNanos");
    final long samplesIntervalNanos = getSamplesIntervalNanos();
    final long samplesWindowShiftSteps = this.samplesWindowShiftSteps;
    final long rightNanos = rightSamplesWindowBoundary(samplesWindowShiftSteps);
    final long leftNanos = rightNanos - samplesIntervalNanos;
    final long count;
    final long effectiveTNanos;
    if (NanosComparator.compare(tNanos, leftNanos) <= 0) {//tNanos is behind the samples window, so return average over all samples
      count = ticksTotalCount();
      effectiveTNanos = rightNanos;
    } else if (NanosComparator.compare(rightNanos, tNanos) <= 0) {//tNanos is ahead of the samples window or exactly on the right border
      count = ticksTotalCount();
      effectiveTNanos = tNanos;
    } else {//tNanos is within the samples window and not on the right border
      final long substractCount = count(tNanos, rightNanos);
      count = ticksTotalCount() - substractCount;
      effectiveTNanos = tNanos;
    }
    return RateMeterMath.rateAverage(effectiveTNanos, samplesIntervalNanos, getStartNanos(), count);
  }

  @Override
  public double rate(final long tNanos) {
    checkArgument(tNanos, "tNanos");
    final double result;
    final long samplesIntervalNanos = getSamplesIntervalNanos();
    final long samplesWindowShiftSteps = this.samplesWindowShiftSteps;
    final long rightNanos = rightSamplesWindowBoundary(samplesWindowShiftSteps);
    final long leftNanos = rightNanos - samplesIntervalNanos;
    if (NanosComparator.compare(tNanos, leftNanos) <= 0) {//tNanos is behind the samples window, so return average over all samples
      result = RateMeterMath.rateAverage(rightNanos, samplesIntervalNanos, getStartNanos(), ticksTotalCount());//this is the same as rateAverage(), or rateAverage(rightNanos) if rightNanos == rightSamplesWindowBoundary()
    } else {//tNanos is within or ahead of the samples window
      final long effectiveLeftNanos = tNanos - samplesIntervalNanos;
      if (NanosComparator.compare(rightNanos, effectiveLeftNanos) <= 0) {//tNanos is way too ahead of the samples window and the are no samples for the requested tNanos
        result = 0;
      } else {
        result = count(effectiveLeftNanos, rightNanos);
      }
    }
    return result;
  }

  public long rightSamplesWindowBoundary(final long samplesWindowShiftSteps) {
    return getStartNanos() + samplesWindowShiftSteps * samplesWindowStepNanos;
  }

  private final long count(final long fromExclusiveNanos, final long toInclusiveNanos) {
    final long fromInclusiveNanos = fromExclusiveNanos + 1;
    final long fromInclusiveSamplesWindowShiftSteps = samplesWindowShiftSteps(fromInclusiveNanos);
    final long toInclusiveSamplesWindowShiftSteps = samplesWindowShiftSteps(toInclusiveNanos);
    long result = 0;
    for (int idx = rightSamplesWindowIdx(fromInclusiveSamplesWindowShiftSteps), i = 0;
         i < toInclusiveSamplesWindowShiftSteps - fromInclusiveSamplesWindowShiftSteps + 1;
         idx = nextSamplesWindowIdx(idx), i++) {
      result += samples[idx];
    }
    return result;
  }

  private final long samplesWindowShiftSteps(final long tNanos) {
    final long samplesWindowShiftNanos = tNanos - getStartNanos();
    return samplesWindowShiftNanos % samplesWindowStepNanos == 0
        ? samplesWindowShiftNanos / samplesWindowStepNanos
        : samplesWindowShiftNanos / samplesWindowStepNanos + 1;
  }

  private final int leftSamplesWindowIdx(final long samplesWindowShiftSteps) {
    return (int)(Math.abs((samplesWindowShiftSteps + samples.length / 2) % samples.length));//the result can not be greater than samples.length, which is int, so it is a safe cast to int
  }

  private final int rightSamplesWindowIdx(final long samplesWindowShiftSteps) {
    return (int)(Math.abs((samplesWindowShiftSteps + samples.length - 1) % samples.length));//the result can not be greater than samples.length, which is int, so it is a safe cast to int
  }

  private final int nextSamplesWindowIdx(final int idx) {
    return (idx + 1) % samples.length;
  }
}