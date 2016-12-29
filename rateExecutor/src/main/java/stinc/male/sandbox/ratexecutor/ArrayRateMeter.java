package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public class ArrayRateMeter extends AbstractRateMeter {
  private static final RateMeterConfig defaultInstance = RateMeterConfig.newBuilder()
      .setTicksCounterSupplier(LongTicksCounter::new)
      .build();

  private final long[] samples;//length is even
  private final long samplesWindowStepNanos;
  private long samplesWindowShiftSteps;

  /**
   * @return A reasonable configuration.
   */
  public static final RateMeterConfig defaultConfig() {
    return defaultInstance;
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
      final long samplesWindowShiftSteps = this.samplesWindowShiftSteps;
      final long rightNanos = rightSamplesWindowBoundary(samplesWindowShiftSteps);
      final long leftNanos = rightNanos - getSamplesIntervalNanos();
      if (NanosComparator.compare(leftNanos, tNanos) < 0) {//tNanos is within or ahead of the samples window
        final long targetSamplesWindowShiftSteps = samplesWindowShiftSteps(tNanos);
        if (targetSamplesWindowShiftSteps > samplesWindowShiftSteps) {//we need to move the samples window
          final long numberOfStepsToMove = targetSamplesWindowShiftSteps - samplesWindowShiftSteps;
          final long numberOfIterations = Math.min(samples.length, numberOfStepsToMove);
          for (int idx = leftSamplesWindowIdx(samplesWindowShiftSteps), i = 0;
               i < numberOfIterations;
               idx = nextSamplesWindowIdx(idx), i++) {//reset moved samples
            samples[idx] = 0;
          }
          this.samplesWindowShiftSteps = targetSamplesWindowShiftSteps;
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
    final long rightNanos = rightSamplesWindowBoundary();
    final long leftNanos = rightNanos - samplesIntervalNanos;
    final long count;
    final long effectiveTNanos;
    if (NanosComparator.compare(tNanos, leftNanos) <= 0) {//tNanos is left from samples window
      count = ticksTotalCount();
      effectiveTNanos = rightNanos;
    } else if (NanosComparator.compare(tNanos, rightNanos) >= 0) {//tNanos is right from samples window or exactly on the right border
      count = ticksTotalCount();
      effectiveTNanos = tNanos;
    } else {//tNanos is within the samples window
      final long samplesWindowShiftSteps = this.samplesWindowShiftSteps;
      long subtractCount = 0;//TODO create count(tNanos, rightNanos)
      final long effectiveSubstractSamplesWindowShiftSteps = samplesWindowShiftSteps(tNanos + samplesIntervalNanos);
      final int rightSamplesIdx = rightSamplesWindowIdx(samplesWindowShiftSteps);
      for (int samplesIdx = leftSamplesWindowIdx(effectiveSubstractSamplesWindowShiftSteps), i = 0;
           i < samples.length;
           samplesIdx = nextSamplesWindowIdx(samplesIdx), i++) {//iterate from leftSamplesIdx (inclusive) to effectiveSubstractRightSamplesIdx (inclusive) but not more than samples.length iterations
        subtractCount += samples[samplesIdx];
        if (samplesIdx == rightSamplesIdx) {
          break;
        }
      }
      count = ticksTotalCount() - subtractCount;
      effectiveTNanos = tNanos;
    }
    return RateMeterMath.rateAverage(effectiveTNanos, samplesIntervalNanos, getStartNanos(), count);
  }

  @Override
  public double rate(final long tNanos) {
    checkArgument(tNanos, "tNanos");
    final double result;
    final long rightNanos = rightSamplesWindowBoundary();
    if (NanosComparator.compare(tNanos, rightNanos) < 0) {
      result = rateAverage();
    } else {
      final long samplesIntervalNanos = getSamplesIntervalNanos();
      final long effectiveRightNanos = NanosComparator.max(rightNanos, tNanos);
      final long effectiveLeftNanos = effectiveRightNanos - samplesIntervalNanos;
      if (NanosComparator.compare(effectiveLeftNanos, rightNanos) >= 0) {//the are no samples for the requested tNanos
        result = 0;
      } else {
        final long effectiveSamplesWindowShiftSteps = samplesWindowShiftSteps(effectiveRightNanos);
        final int effectiveRightSamplesIdx = rightSamplesWindowIdx(effectiveSamplesWindowShiftSteps);
        long count = 0;
        for (int samplesIdx = leftSamplesWindowIdx(effectiveSamplesWindowShiftSteps), i = 0;
             i < samples.length;
             samplesIdx = nextSamplesWindowIdx(samplesIdx), i++) {//iterate from leftSamplesIdx (inclusive) to effectiveRightSamplesIdx (inclusive) but not more than samples.length iterations
          count += samples[samplesIdx];
          if (samplesIdx == effectiveRightSamplesIdx) {
            break;
          }
        }
        result = count;
      }
    }
    return result;
  }

  public long rightSamplesWindowBoundary(final long samplesWindowShiftSteps) {
    return getStartNanos() + samplesWindowShiftSteps * samplesWindowStepNanos;
  }

  private final long samplesWindowShiftSteps(final long tNanos) {
    final long samplesWindowShiftNanos = tNanos - getStartNanos();
    return samplesWindowShiftNanos % samplesWindowStepNanos == 0
        ? samplesWindowShiftNanos / samplesWindowStepNanos
        : samplesWindowShiftNanos / samplesWindowStepNanos + 1;
  }

  private final int leftSamplesWindowIdx(final long samplesWindowShiftSteps) {
    return (int)((samplesWindowShiftSteps + samples.length / 2) % samples.length);//the result can not be greater than samples.length, which is int, so it is a safe cast to int
  }

  private final int rightSamplesWindowIdx(final long samplesWindowShiftSteps) {
    return (int)((samplesWindowShiftSteps + samples.length - 1) % samples.length);//the result can not be greater than samples.length, which is int, so it is a safe cast to int
  }

  private final int nextSamplesWindowIdx(final int idx) {
    return (idx + 1) % samples.length;
  }
}