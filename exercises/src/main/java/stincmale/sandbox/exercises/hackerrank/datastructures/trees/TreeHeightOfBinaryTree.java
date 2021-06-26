package stincmale.sandbox.exercises.hackerrank.datastructures.trees;

import java.util.Scanner;

/**
 * <a href="https://www.hackerrank.com/challenges/tree-height-of-a-binary-tree">
 * Tree: Height of a Binary Tree</a>.
 */
final class TreeHeightOfBinaryTree {
    public static void main(final String[] args) {
        try (Scanner in = new Scanner(System.in)) {
            int t = in.nextInt();
            Node root = null;
            while (t-- > 0) {
                final int data = in.nextInt();
                root = insert(root, data);
            }
            assert root != null;
            final int height = Solution.height(root);
            System.out.println(height);
        }
    }

    static Node insert(final Node root, final int data) {
        if (root == null) {
            return new Node(data);
        } else {
            final Node cur;
            if (data <= root.data) {
                cur = insert(root.left, data);
                root.left = cur;
            } else {
                cur = insert(root.right, data);
                root.right = cur;
            }
            return root;
        }
    }

    private TreeHeightOfBinaryTree() {
        throw new AssertionError();
    }

    static final class Solution {
        static int height(final Node root) {
            final int result;
            if (root.left == null && root.right == null) {
                // leaf
                result = 0;
            } else {
                final int lh = root.left == null ? 0 : height(root.left);
                final int rh = root.right == null ? 0 : height(root.right);
                result = 1 + Math.max(lh, rh);
            }
            return result;
        }

        private Solution() {
            throw new AssertionError();
        }
    }

    static class Node {
        Node left;
        Node right;
        int data;

        Node(final int data) {
            this.data = data;
            left = null;
            right = null;
        }
    }
}
