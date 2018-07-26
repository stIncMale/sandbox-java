package stincmale.sandbox.benchmarks;

import java.util.concurrent.ThreadLocalRandom;

class Test {//TODO check on ARM (try running on Android)
  static final int NUMBER_OF_EXPERIMENTS = 100_000_000;

  final State state = new State();

  public static void main(String... args) throws InterruptedException {
    System.out.println();
    System.out.println("t1 start");
    int v = new Test().t1();
    System.out.println("t1 end " + v);
  }

  int t1() throws InterruptedException {
    state.v = 1;
    Thread t2 = new Thread(new Runnable() {
      @Override
      public void run() {
        t(2);
      }
    });
    Thread t3 = new Thread(new Runnable() {
      @Override
      public void run() {
        t(3);
      }
    });
    Thread t4 = new Thread(new Runnable() {
      @Override
      public void run() {
        t(4);
      }
    });
    t2.start();
    t3.start();
    t4.start();
    for (int experimentI = 0; experimentI < NUMBER_OF_EXPERIMENTS; experimentI++) {
      state.v = state.v + 1;
      tryReschedule(experimentI);
    }
    t2.join();
    t3.join();
    t4.join();
    return state.v;
  }

  void t(int i) {
    System.out.println("t" + i + " start");
    try {
      System.out.println("t" + i + " end " + Test.this.t());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

  long t() throws InterruptedException {
    ThreadLocalRandom rnd = ThreadLocalRandom.current();
    int prev = state.v;
    long s = 0;
    for (int experimentI = 0; experimentI < NUMBER_OF_EXPERIMENTS; experimentI++) {
      int rndv = rnd.nextInt(100);
      if (rndv > 0) {
        int v = read(state, prev, rndv);
        if (v < prev) {
          System.out.println("bingo " + prev + ' ' + v);
          System.exit(0);
        }
        prev = v;
        s += v;
      }
      tryReschedule(experimentI);
    }
    return s;
  }

  int read(State s, int prev, int rndv) {
    if (rndv > 1) {
      return s.v;
    } else {
      int k = rndv;//consume rndv in a way that generates huge amount of bytecode
      if (k > 0) {
        return s.v;
      } else {
        return Math.max(s.v, prev);
      }
    }
  }

  static void tryReschedule(int i) throws InterruptedException {
    if (i % 10_000 == 0) {
      Thread.sleep(1);
    } else if (i % 500 == 0) {
      Thread.yield();
    }
  }

  static class State {
    int v;
  }
}