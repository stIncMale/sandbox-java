package stincmale.sandbox.exercises.hackerrank.datastructures.trees;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

/**
 * <a href="https://www.hackerrank.com/challenges/tree-level-order-traversal">
 * Tree: Level Order Traversal</a>.
 */
final class TreeLevelOrderTraversal {
    public static void main(final String[] args) {
        try (Scanner in = new Scanner(System.in)) {
            int t = in.nextInt();
            Node root = null;
            while (t-- > 0) {
                final int data = in.nextInt();
                root = insert(root, data);
            }
            assert root != null;
            Solution.levelOrder(root);
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

    private TreeLevelOrderTraversal() {
        throw new AssertionError();
    }

    static final class Solution {
        static void levelOrder(final Node root) {
            final StringBuilder s = new StringBuilder();
            s.append(root.data);
            List<Node> prevLevel = Collections.singletonList(root);
            while (!prevLevel.isEmpty()) {
                final List<Node> level = new ArrayList<>();
                prevLevel.forEach(node -> {
                    if (node.left != null) {
                        level.add(node.left);
                        s.append(" ")
                                .append(node.left.data);
                    }
                    if (node.right != null) {
                        level.add(node.right);
                        s.append(" ")
                                .append(node.right.data);
                    }
                });
                prevLevel = level;
            }
            System.out.println(s);
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
