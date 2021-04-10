package stincmale.sandbox.exercises.hackerrank.datastructures.stacks;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Scanner;

/**
 * <a href="https://www.hackerrank.com/challenges/largest-rectangle">Largest Rectangle</a>
 */
public class LargestRectangle {
  static long largestRectangle(int[] a) {
    int i = 0;
    long maxArea = 0;
    Deque<Element> stack = new ArrayDeque<>();
    for (; i < a.length; i++) {
      int h = a[i];
      Element rightmost = stack.peek();
      if (rightmost == null || h > rightmost.h) {
        stack.push(new Element(i, h));
      } else if (h < rightmost.h) {
        long localMaxArea = shrinkPushAndFindMaxArea(stack, new Element(i, h));
        maxArea = Math.max(maxArea, localMaxArea);
      } else {// h == rightmost.h
        // nothing to do
      }
    }
    long localMaxArea = shrinkPushAndFindMaxArea(stack, new Element(i, 0));
    maxArea = Math.max(maxArea, localMaxArea);
    return maxArea;
  }

  static int shrinkPushAndFindMaxArea(Deque<Element> stack, Element newElement) {
    int localMaxArea = 0;
    boolean pushNewElement = true;
    int newLeftmostIdx = -1;
    while (!stack.isEmpty()) {
      Element rightmost = stack.peek();
      if (rightmost.h < newElement.h) {
        break;
      } else if (rightmost.h == newElement.h) {
        pushNewElement = false;
        break;
      } else {
        stack.pop();
        newLeftmostIdx = rightmost.leftmostIdx;
        int area = rightmost.h * (newElement.leftmostIdx - rightmost.leftmostIdx);
        localMaxArea = Math.max(localMaxArea, area);
      }
    }
    if (pushNewElement && newElement.h > 0) {
      newLeftmostIdx = stack.isEmpty()
          ? 0
          : newLeftmostIdx < 0
              ? newElement.leftmostIdx
              : newLeftmostIdx;
      stack.push(new Element(newLeftmostIdx, newElement.h));
    }
    return localMaxArea;
  }

  static class Element {
    int leftmostIdx;
    int h;

    Element(int leftmostIdx, int h) {
      this.leftmostIdx = leftmostIdx;
      this.h = h;
    }
  }

  public static void main(String[] args) {
    try (Scanner in = new Scanner(System.in)) {
      int n = in.nextInt();
      int[] a = new int[n];
      for (int i = 0; i < n; i++) {
        a[i] = in.nextInt();
      }
      System.out.println(largestRectangle(a));
    }
  }
}
