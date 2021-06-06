package stincmale.sandbox.exercises.hackerrank.datastructures.trees;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

/**
 * <a href="https://www.hackerrank.com/challenges/tree-level-order-traversal">
 * Tree: Level Order Traversal</a>
 */
final class TreeLevelOrderTraversal {
    public static void main(String[] args) {
        try (Scanner in = new Scanner(System.in)) {
            int t = in.nextInt();
            Node root = null;
            while (t-- > 0) {
                int data = in.nextInt();
                root = insert(root, data);
            }
            assert root != null;
            Solution.levelOrder(root);
        }
    }

    static Node insert(Node root, int data) {
        if (root == null) {
            return new Node(data);
        } else {
            Node cur;
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

    static class Solution {
        static void levelOrder(Node root) {
            StringBuilder s = new StringBuilder();
            s.append(root.data);
            List<Node> prevLevel = Collections.singletonList(root);
            while (!prevLevel.isEmpty()) {
                List<Node> level = new ArrayList<>();
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
    }

    static class Node {
        Node left;
        Node right;
        int data;

        Node(int data) {
            this.data = data;
            left = null;
            right = null;
        }
    }
}
