package stinc.male.sandbox.ratexecutor;

import java.time.Duration;
import java.util.concurrent.locks.StampedLock;
import javax.annotation.concurrent.ThreadSafe;
import static com.sun.xml.internal.ws.client.ContentNegotiation.optimistic;
import static stinc.male.sandbox.ratexecutor.Preconditions.checkNotNull;

@ThreadSafe
public class ArrayRateMeter extends AbstractRateMeter {
  private static final int MAX_SAMPLES_ARRAY_LENGTH = 1_000;

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
    int samplesArrayLength;
    for (samplesArrayLength = (int)(samplesIntervalNanos / sensitivityNanos);
        samplesArrayLength > 0 && samplesArrayLength <= MAX_SAMPLES_ARRAY_LENGTH + 1
            && samplesIntervalNanos / samplesArrayLength * samplesArrayLength == samplesIntervalNanos;
        samplesArrayLength++);
    if (samplesArrayLength > MAX_SAMPLES_ARRAY_LENGTH) {
      throw new IllegalArgumentException(String.format(
          "The specified samplesInterval %snanos and timeSensitivity %snanos can not be used together because samplesInterval can not be devided evenly by timeSensitivity",
          samplesArrayLength, sensitivityNanos));
    }
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
    for (int idx = 0; idx < samples.length; idx++) {
      result += samples[idx].get();
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
        final long samplesWindowShiftSteps = samplesWindowStepNanos;
        final long targetSamplesWindowShiftSteps = (tNanos - getStartNanos()) / samplesWindowStepNanos + 1;
        if (targetSamplesWindowShiftSteps <= samplesWindowShiftSteps) {//no need to move the samples window
          samples[samplesIdx(targetSamplesWindowShiftSteps)].add(count);
        } else {//we need to move the samples window
          moveSamplesWindowLock.try
          this.samplesWindowShiftSteps = targetSamplesWindowShiftSteps;
          final long moveRightSteps = targetSamplesWindowShiftSteps - samplesWindowShiftSteps;
          if (moveRightSteps < samples.length) {//some samples are intact

          } else {//reset all samples
            for (int idx = 0; idx < samples.length; idx++) {
              //reset samples[idx]
            }
          }
        }
      }
      getTicksTotalCounter().add(count);
    }
  }

  @Override
  public double rate(final long tNanos) {
    return 0;
  }

  private final int samplesIdx(final long samplesWindowShiftSteps) {
    return (int)((samplesWindowShiftSteps + samples.length - 1) % samples.length);//the result can not be greater than samples.length, which is int, so it is a safe cast to int
  }
}