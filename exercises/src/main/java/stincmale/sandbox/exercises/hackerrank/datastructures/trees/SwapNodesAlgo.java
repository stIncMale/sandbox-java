package stincmale.sandbox.exercises.hackerrank.datastructures.trees;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.Scanner;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * <a href="https://www.hackerrank.com/challenges/swap-nodes-algo">Swap Nodes [Algo]</a>.
 */
final class SwapNodesAlgo {
    public static void main(final String[] args) {
        try (Scanner in = new Scanner(System.in)) {
            final int numberOfNodes = in.nextInt();
            final SortedMap<Integer, Node> nodes = new TreeMap<>();
            final AtomicInteger depth = new AtomicInteger(1);
            for (int i = 1; i <= 2 * numberOfNodes; i++) {
                final int nodeIdx = i % 2 == 0
                        ? i / 2
                        : 1 + i / 2;
                final Node node = nodes.computeIfAbsent(nodeIdx, idx -> new Node(idx, depth.get()));
                depth.set(node.d);
                final int childNodeIdx = in.nextInt();
                if (childNodeIdx > 1) {
                    nodes.computeIfAbsent(childNodeIdx, idx -> new Node(idx, depth.get() + 1));
                    if (i % 2 != 0) {
                        // left
                        node.l = childNodeIdx;
                    } else {
                        // right
                        node.r = childNodeIdx;
                    }
                }
            }
            final NavigableMap<Integer, List<Node>> layers = createLayers(nodes);
            final int numberOfSwapOperations = in.nextInt();
            for (int i = 0; i < numberOfSwapOperations; i++) {
                final int k = in.nextInt();
                swapOperation(layers, k);
                printTraverse(nodes);
            }
        }
    }

    private SwapNodesAlgo() {
        throw new AssertionError();
    }

    static NavigableMap<Integer, List<Node>> createLayers(final SortedMap<Integer, Node> nodes) {
        final NavigableMap<Integer, List<Node>> result = new TreeMap<>();
        nodes.values()
                .forEach(node -> {
                    final int depth = node.d;
                    final List<Node> layer = result.computeIfAbsent(depth, d -> new ArrayList<>());
                    layer.add(node);
                });
        return result;
    }

    static List<Node> traverse(final SortedMap<Integer, Node> nodes, final Node root) {
        final List<Node> result = new ArrayList<>();
        if (root.l > 1) {
            result.addAll(traverse(nodes, nodes.get(root.l)));
        }
        result.add(root);
        if (root.r > 1) {
            result.addAll(traverse(nodes, nodes.get(root.r)));
        }
        return result;
    }

    static void printTraverse(final SortedMap<Integer, Node> nodes) {
        System.out.println(traverse(nodes, nodes.get(nodes.firstKey()))
                .stream()
                .map(node -> String.valueOf(node.i))
                .collect(Collectors.joining(" ")));
    }

    static void swapping(final NavigableMap<Integer, List<Node>> layers, final int layerDepth) {
        final List<Node> layer = layers.get(layerDepth);
        layer.forEach(node -> {
            final int swap = node.r;
            node.r = node.l;
            node.l = swap;
        });
    }

    static void swapOperation(final NavigableMap<Integer, List<Node>> layers, final int k) {
        final int maxDepth = layers.lastKey();
        for (int i = 1; i * k <= maxDepth; i++) {
            swapping(layers, i * k);
        }
    }

    static class Node {
        int i;
        int l;
        int r;
        int d;

        Node(final int i, final int d) {
            this.i = i;
            l = -1;
            r = -1;
            this.d = d;
        }
    }
}
