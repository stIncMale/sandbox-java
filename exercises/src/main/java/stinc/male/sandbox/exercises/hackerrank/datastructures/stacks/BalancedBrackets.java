package stinc.male.sandbox.exercises.hackerrank.datastructures.stacks;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Scanner;

/**
 * <a href="https://www.hackerrank.com/challenges/balanced-brackets">Balanced Brackets</a>
 */
public class BalancedBrackets {
  static String isBalanced(String s) {
    String result = "YES";
    if (s == null || s.length() == 0) {
      //nothing to do
    } else if (s.length() % 2 != 0) {
      result = "NO";
    } else {
      Deque<Character> stack = new ArrayDeque<>();
      for (int j = 0; j < s.length(); j++) {
        char c = s.charAt(j);
        if (c == '{' || c == '[' || c == '(') {//opening
          stack.push(c);
        } else {//closing
          if (stack.isEmpty()) {
            result = "NO";
            break;
          } else {
            char top = stack.peek();
            if ((top == '{' && c != '}')
                || (top == '[' && c != ']')
                || (top == '(' && c != ')')) {
              result = "NO";
              break;
            } else {
              stack.pop();
            }
          }
        }
      }
      if (!stack.isEmpty()) {
        result = "NO";
      }
    }
    return result;
  }

  public static void main(String[] args) {
    try (Scanner in = new Scanner(System.in)) {
      int t = in.nextInt();
      for(int a0 = 0; a0 < t; a0++){
        String s = in.next();
        System.out.println(isBalanced(s));
      }
    }
  }
}