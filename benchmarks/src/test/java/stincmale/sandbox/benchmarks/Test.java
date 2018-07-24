package stincmale.sandbox.benchmarks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadLocalRandom;

class Test {//TODO check on ARM (try running on Android)
  static final int NUMBER_OF_EXPERIMENTS = 50_000;
  static final int NUMBER_OF_READS = 5000;

  final State state = new State();
  final CyclicBarrier cb = new CyclicBarrier(2);

  public static void main(String... args) throws BrokenBarrierException, InterruptedException {
    System.out.println();
    System.out.println("start");
    new Test().t1();
    System.out.println("end");
  }

  void t1() throws BrokenBarrierException, InterruptedException {
    Thread t2 = new Thread(this::t2);
    t2.start();
    for (int experimentI = 0; experimentI < NUMBER_OF_EXPERIMENTS; experimentI++) {
      state.v = state.v == null
          ? new Integer(1)
          : new Integer(state.v.v + 1);
      cb.await();
    }
    t2.join();
  }

  void t2() {
    Collection<Integer> reads = new ArrayList<>();
    for (int experimentI = 0; experimentI < NUMBER_OF_EXPERIMENTS; experimentI++) {
      experiment(reads);
      try {
        cb.await();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      } catch (BrokenBarrierException e) {
        throw new RuntimeException(e);
      }
    }
  }

  void experiment(Collection<Integer> reads) {
    reads.clear();
    for (int readI = 0; readI < NUMBER_OF_READS; readI++) {
      if (ThreadLocalRandom.current().nextBoolean()) {
        Thread.yield();
      }
      readFromState(state, reads);
    }
    analyseAndPrintIfBackForth(reads);
  }

  void readFromState(State s, Collection<Integer> reads) {
    reads.add(s.v);
  }

  void analyseAndPrintIfDifferent(Collection<Integer> reads) {
    if (reads.stream()
        .distinct()
        .mapToInt(any -> 1)
        .sum() > 1) {
      System.out.println(reads);
    }
  }

  void analyseAndPrintIfBackForth(Collection<Integer> reads) {
    int backForce = 0;
    int pv = -1;
    for (Integer r : reads) {
      int v = r.v;
      if (v != pv) {
        backForce++;
        pv = r.v;
      }
    }
    if (backForce > 2) {
      System.out.println(reads);
    }
  }

  static class State {
    Integer v;
  }

  static class Integer {
    int v;

    Integer(int v) {
      this.v = v;
    }

    @Override
    public String toString() {
      return String.valueOf(v);
    }
  }
}