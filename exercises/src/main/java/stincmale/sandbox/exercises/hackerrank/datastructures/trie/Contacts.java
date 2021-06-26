package stincmale.sandbox.exercises.hackerrank.datastructures.trie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * <a href="https://www.hackerrank.com/challenges/contacts">Contacts</a>.
 */
final class Contacts {
    public static void main(final String[] args) {
        try (Scanner in = new Scanner(System.in)) {
            final int numberOfOps = in.nextInt();
            final Node trie = new Node();
            for (int i = 0; i < numberOfOps; i++) {
                final String opName = in.next();
                final String opArgument = in.next();
                if (opName.length() == 3) {
                    // add opName
                    add(opArgument, trie);
                } else {
                    // find opName
                    final int opResult = find(opArgument, trie);
                    if (i == numberOfOps - 1) {
                        // last operation
                        System.out.print(opResult);
                    } else {
                        System.out.println(opResult);
                    }
                }
            }
        }
    }

    static void add(final String name, final Node trie) {
        final List<Node> path = new ArrayList<>();
        Node node = trie;
        for (int i = 0; i < name.length(); i++) {
            final char c = name.charAt(i);
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

    static int find(final String partial, final Node trie) {
        int result = 0;
        Node node = trie;
        for (int i = 0; i < partial.length(); i++) {
            final char c = partial.charAt(i);
            final Node current;
            if (node != null) {
                current = node.children.get(c);
            } else {
                // partial has not been found
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

    private Contacts() {
        throw new AssertionError();
    }

    static class Node {
        char c;
        Map<Character, Node> children;
        int count;

        Node() {
            this('-');
        }

        Node(final char c) {
            this.c = c;
            children = new HashMap<>();
            this.count = 0;
        }
    }
}
