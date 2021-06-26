package stincmale.sandbox.exercises.hackerrank.datastructures.heap;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 * <a href="https://www.hackerrank.com/challenges/find-the-running-median">
 * Find the Running Median</a>.
 */
final class FindTheRunningMedian {
    public static void main(final String[] args) {
        try (Scanner in = new Scanner(System.in)) {
            final int numberOfNumbers = in.nextInt();
            final List<Integer> numbers = new ArrayList<>();
            for (int i = 0; i < numberOfNumbers; i++) {
                final int number = in.nextInt();
                add(numbers, number);
                final double median = numbers.size() == 1
                        ? numbers.get(0)
                        : numbers.size() % 2 == 0
                                ? (double) (numbers.get((numbers.size() - 1) / 2)
                                + numbers.get(1 + (numbers.size() - 1) / 2)) / 2
                                : numbers.get(numbers.size() / 2);
                if (i < numberOfNumbers - 1) {
                    System.out.printf(Locale.ROOT, "%.1f\n", median);
                } else {
                    System.out.printf(Locale.ROOT, "%.1f", median);
                }
            }
        }
    }

    private FindTheRunningMedian() {
        throw new AssertionError();
    }

    static void add(final List<Integer> list, final int v) {
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
                final int anchorIdx = l + (r - l) / 2;
                if (v < list.get(anchorIdx)) {
                    r = anchorIdx;
                } else if (v > list.get(anchorIdx)) {
                    l = anchorIdx;
                } else {
                    // v == list.get(anchorIdx)
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
