package stincmale.sandbox.exercises.hackerrank.datastructures.stacks;

import java.util.Scanner;

/**
 * <a href="https://www.hackerrank.com/challenges/and-xor-or">AND xor OR</a>.
 */
final class AndXorOr {
    public static void main(final String[] args) {
        try (Scanner in = new Scanner(System.in)) {
            final int n = in.nextInt();
            final int[] a = new int[n];
            for (int i = 0; i < n; i++) {
                a[i] = in.nextInt();
            }
            int maxS = 0;
            for (int i = 0; i < a.length - 1; i++) {
                for (int j = i + 1; j < a.length; j++) {
                    maxS = Math.max(maxS, s(a[i], a[j]));
                    if (a[j] <= a[i] || (j < a.length - 1 && a[j + 1] > a[j])) {
                        break;
                    }
                }
            }
            System.out.print(maxS);
        }
    }

    private AndXorOr() {
        throw new AssertionError();
    }

    static int s(final int m1, final int m2) {
        // same as ((m1 & m2) ^ (m1 | m2)) & (m1 ^ m2)
        return m1 ^ m2;
    }
}
