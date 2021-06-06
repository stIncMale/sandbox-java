package stincmale.sandbox.exercises.hackerrank.datastructures.trie;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * <a href="https://www.hackerrank.com/challenges/no-prefix-set">No Prefix Set</a>
 */
final class NoPrefixSet {
    public static void main(String[] args) {
        try (Scanner in = new Scanner(System.in)) {
            int n = in.nextInt();
            Node trie = new Node();
            for (int i = 0; i < n; i++) {
                String s = in.next();
                Node node = trie;
                for (int j = 0; j < s.length(); j++) {
                    char c = s.charAt(j);
                    boolean leaf = j == s.length() - 1;
                    Node childNode = node.children.get(c);
                    if (childNode == null) {
                        childNode = new Node(c);
                        node.children.put(childNode.c, childNode);
                    } else {
                        if (childNode.leaf || leaf) {
                            System.out.println("BAD SET");
                            System.out.println(s);
                            return;
                        }
                    }
                    childNode.leaf = leaf;
                    node = childNode;
                }
            }
            System.out.println("GOOD SET");
        }
    }

    private NoPrefixSet() {
        throw new AssertionError();
    }

    static class Node {
        char c;
        Map<Character, Node> children;
        boolean leaf;

        Node() {
            this('-');
        }

        Node(char c) {
            this.c = c;
            children = new HashMap<>();
            leaf = false;
        }
    }
}
