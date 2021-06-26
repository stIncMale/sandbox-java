package stincmale.sandbox.exercises.hackerrank.datastructures.stacks;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Scanner;

/**
 * <a href="https://www.hackerrank.com/challenges/balanced-brackets">Balanced Brackets</a>.
 */
final class BalancedBrackets {
    static String isBalanced(final String s) {
        String result = "YES";
        final String no = "NO";
        if (s == null || s.length() == 0) {
            // nothing to do
        } else if (s.length() % 2 != 0) {
            result = no;
        } else {
            final Deque<Character> stack = new ArrayDeque<>();
            for (int j = 0; j < s.length(); j++) {
                final char c = s.charAt(j);
                if (c == '{' || c == '[' || c == '(') {
                    // opening
                    stack.push(c);
                } else {
                    // closing
                    if (stack.isEmpty()) {
                        result = no;
                        break;
                    } else {
                        final char top = stack.peek();
                        if ((top == '{' && c != '}')
                                || (top == '[' && c != ']')
                                || (top == '(' && c != ')')) {
                            result = no;
                            break;
                        } else {
                            stack.pop();
                        }
                    }
                }
            }
            if (!stack.isEmpty()) {
                result = no;
            }
        }
        return result;
    }

    private BalancedBrackets() {
        throw new AssertionError();
    }

    public static void main(final String[] args) {
        try (Scanner in = new Scanner(System.in)) {
            final int t = in.nextInt();
            for (int a0 = 0; a0 < t; a0++) {
                final String s = in.next();
                System.out.println(isBalanced(s));
            }
        }
    }
}
