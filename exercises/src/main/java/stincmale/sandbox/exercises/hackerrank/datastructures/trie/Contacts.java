package stincmale.sandbox.exercises.hackerrank.datastructures.trie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * <a href="https://www.hackerrank.com/challenges/contacts">Contacts</a>
 */
public class Contacts {
  public static void main(String[] args) {
    try (Scanner in = new Scanner(System.in)) {
      int numberOfOps = in.nextInt();
      Node trie = new Node();
      for (int i = 0; i < numberOfOps; i++) {
        String opName = in.next();
        String opArgument = in.next();
        if (opName.length() == 3) {//add opName
          add(opArgument, trie);
        } else {//find opName
          final int opResult = find(opArgument, trie);
          if (i == numberOfOps - 1) {//last operation
            System.out.print(opResult);
          } else {
            System.out.println(opResult);
          }
        }
      }
    }
  }

  static void add(String name, Node trie) {
    List<Node> path = new ArrayList<>();
    Node node = trie;
    for (int i = 0; i < name.length(); i++) {
      char c = name.charAt(i);
      Node child = node.children.get(c);
      if (child == null) {
        child = new Node(c);
        node.children.put(c, child);
      }
      node = child;
      path.add(child);
    }
    path.forEach(n -> n.count++);
    if (!path.isEmpty()) {
      trie.count++;
    }
  }

  static int find(String partial, Node trie) {
    int result = 0;
    Node node = trie;
    for (int i = 0; i < partial.length(); i++) {
      char c = partial.charAt(i);
      Node current;
      if (node != null) {
        current = node.children.get(c);
      } else {//partial has not been found
        current = null;
      }
      if (current != null) {
        result = current.count;
        node = current;
      } else {
        result = 0;
        break;
      }
    }
    return result;
  }

  static class Node {
    char c;
    Map<Character, Node> children;
    int count;

    Node() {
      this('-');
    }

    Node(char c) {
      this.c = c;
      children = new HashMap<>();
      this.count = 0;
    }
  }
}
