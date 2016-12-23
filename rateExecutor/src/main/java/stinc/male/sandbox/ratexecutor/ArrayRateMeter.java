package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
public class ArrayRateMeter extends AbstractRateMeter {
  private final TicksCounter[] samples;
  private final long samplesWindowStepNanos;
  private long samplesWindowShiftSteps;

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
    final int samplesArrayLength = (int)(samplesIntervalNanos / sensitivityNanos);
    Preconditions.checkArgument(samplesIntervalNanos / samplesArrayLength * samplesArrayLength == samplesIntervalNanos, "samplesInterval",
        () -> String.format(
            "The specified samplesInterval %snanos and timeSensitivity %snanos can not be used together because samplesInterval can not be devided evenly by timeSensitivity",
            samplesArrayLength, sensitivityNanos));
    samplesWindowStepNanos = samplesIntervalNanos / samplesArrayLength;
    samples = new TicksCounter[samplesArrayLength];
    for (int idx = 0; idx < samples.length; idx++) {
      samples[idx] = config.getTicksCounterSupplier().apply(0L);
    }
    samplesWindowShiftSteps = 0;
  }

  @Override
  public long rightSamplesWindowBoundary() {
    return getStartNanos() + samplesWindowShiftSteps * samplesWindowStepNanos;
  }

  @Override
  public long ticksCount() {
    long result = 0;
    for (int samplesIdx = 0; samplesIdx < samples.length; samplesIdx++) {
      result += samples[samplesIdx].get();
    }
    return result;
  }

  @Override
  public void tick(final long count, final long tNanos) {
    checkArgument(tNanos, "tNanos");
    if (count != 0) {
      final long rightNanos = rightSamplesWindowBoundary();
      final long leftNanos = rightNanos - getSamplesIntervalNanos();
      if (NanosComparator.compare(leftNanos, tNanos) < 0) {//tNanos is not behind the samples window
        final long samplesWindowShiftSteps = this.samplesWindowShiftSteps;
        final long targetSamplesWindowShiftSteps = samplesWindowShiftSteps(tNanos);
        if (targetSamplesWindowShiftSteps > samplesWindowShiftSteps) {//we need to move the samples window
          for (int samplesIdx = leftSamplesIdx(samplesWindowShiftSteps), i = 0;
              i < Math.min(samples.length, targetSamplesWindowShiftSteps - samplesWindowShiftSteps);
              samplesIdx = nextSamplesIdx(samplesIdx), i++) {//reset moved samples
            samples[samplesIdx].reset();
          }
          this.samplesWindowShiftSteps = targetSamplesWindowShiftSteps;
        }
        samples[rightSamplesIdx(targetSamplesWindowShiftSteps)].add(count);
      }
      getTicksTotalCounter().add(count);
    }
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
      if (effectiveLeftNanos >= rightNanos) {//the are no samples for the requested tNanos
        result = 0;
      } else {
        final long effectiveSamplesWindowShiftSteps = samplesWindowShiftSteps(effectiveRightNanos);
        long count = 0;
        for (int samplesIdx = leftSamplesIdx(effectiveSamplesWindowShiftSteps), i = 0;
             i < effectiveSamplesWindowShiftSteps;
             samplesIdx = nextSamplesIdx(samplesIdx), i++) {
          count += samples[samplesIdx].get();
        }
        result = count;
      }
    }
    return result;
  }

  private final long samplesWindowShiftSteps(final long tNanos) {
    return (tNanos - getStartNanos()) / samplesWindowStepNanos + 1;
  }

  private final int rightSamplesIdx(final long samplesWindowShiftSteps) {
    return (int)((samplesWindowShiftSteps + samples.length - 1) % samples.length);//the result can not be greater than samples.length, which is int, so it is a safe cast to int
  }

  private final int leftSamplesIdx(final long samplesWindowShiftSteps) {
    return (int)((samplesWindowShiftSteps + samples.length) % samples.length);//the result can not be greater than samples.length, which is int, so it is a safe cast to int
  }

  private final int nextSamplesIdx(final int samplesIdx) {
    return (samplesIdx + samples.length + 1) % samples.length;
  }
}