package stinc.male.sandbox.exercises.hackerrank;

import java.util.*;

/**
 * <a href="https://www.hackerrank.com/challenges/balanced-brackets">Balanced Brackets</a>
 */
public class BalancedBrackets {
  public static void main(String[] args) {
    Scanner in = new Scanner(System.in);
    int numberOfStrings = in.nextInt();
    for(int i = 0; i < numberOfStrings; i++){
      String s = in.next();
      String result = "YES";
      if (s == null || s.length() == 0) {
        //nothing to do
      } else if (s.length() % 2 != 0) {
        result = "NO";
      } else {
        Stack<Character> stack = new Stack<>();
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
      if (i < numberOfStrings - 1) {
        System.out.println(result);
      } else {
        System.out.print(result);
      }
    }
    in.close();
  }
}