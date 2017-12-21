package stinc.male.sandbox.exercises.hackerrank.datastructures.heap;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * <a href="https://www.hackerrank.com/challenges/find-the-running-median">Find the Running Median</a>
 */
public class FindTheRunningMedian {
  public static void main(String[] args) {
    try (Scanner in = new Scanner(System.in)) {
      int numberOfNumbers = in.nextInt();
      List<Integer> numbers = new ArrayList<>();
      for (int i = 0; i < numberOfNumbers; i++) {
        int number = in.nextInt();
        add(numbers, number);
        double median = numbers.size() == 1
            ? numbers.get(0)
            : numbers.size() % 2 == 0
                ? (double)(numbers.get((numbers.size() - 1) / 2) + numbers.get(1 + (numbers.size() - 1) / 2)) / 2
                : numbers.get(numbers.size() / 2);
        if (i < numberOfNumbers - 1) {
          System.out.printf("%.1f\n", median);
        } else {
          System.out.printf("%.1f", median);
        }
      }
    }
  }

  static void add(List<Integer> list, int v) {
    if (list.isEmpty()) {
      list.add(v);
    } else if (v >= list.get(list.size() - 1)) {
      list.add(v);
    } else if (v <= list.get(0)) {
      list.add(0, v);
    } else {
      int l = 0;
      int r = list.size() - 1;
      while (true) {
        int anchorIdx = l + (r - l) / 2;
        if (v < list.get(anchorIdx)) {
          r = anchorIdx;
        } else if (v > list.get(anchorIdx)) {
          l = anchorIdx;
        } else {//v == list.get(anchorIdx)
          list.add(anchorIdx + 1, v);
          break;
        }
        if (r - l <= 1) {
          list.add(l + 1, v);
          break;
        }
      }
    }
  }
}