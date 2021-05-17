package stincmale.sandbox.exercises.hackerrank.datastructures.trees;

import java.util.Scanner;

/**
 * <a href="https://www.hackerrank.com/challenges/tree-height-of-a-binary-tree">Tree: Height of a Binary Tree</a>
 */
final class TreeHeightOfABinaryTree {
  public static void main(String[] args) {
    try (Scanner in = new Scanner(System.in)) {
      int t = in.nextInt();
      Node root = null;
      while (t-- > 0) {
        int data = in.nextInt();
        root = insert(root, data);
      }
      assert root != null;
      int height = Solution.height(root);
      System.out.println(height);
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

  private TreeHeightOfABinaryTree() {
    throw new AssertionError();
  }

  static class Solution {
    static int height(Node root) {
      int result;
      if (root.left == null && root.right == null) {// leaf
        result = 0;
      } else {
        int lh = root.left == null ? 0 : height(root.left);
        int rh = root.right == null ? 0 : height(root.right);
        result = 1 + Math.max(lh, rh);
      }
      return result;
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
