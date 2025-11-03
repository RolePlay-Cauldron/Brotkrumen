package com.github.roleplaycauldron.brotkrumen.graph.search.impl;

import com.github.roleplaycauldron.brotkrumen.graph.Edge;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.TeleportRules;
import com.github.roleplaycauldron.brotkrumen.graph.search.PathAlgorithm;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Abstract base class for the different shortest path algorithms.
 */
abstract class AbstractShortestPath implements PathAlgorithm {

    /**
     * The default constructor.
     */
    public AbstractShortestPath() {
        // Empty
    }

    @Override
    public List<Node> findPath(final Graph graph, final int start, final int goal, final Predicate<Edge> edgeFilter, final TeleportRules rules) {
        if (isMissingNode(graph, start, goal)) {
            return List.of();
        }

        final Predicate<Edge> filter = normalizeFilter(edgeFilter);
        final Map<Integer, Double> gScore = new HashMap<>();
        final Map<Integer, Integer> parent = new HashMap<>();

        final Comparator<int[]> comparator = Comparator
                .<int[]>comparingDouble(a -> priorityScore(a[0], gScore))
                .thenComparingInt(a -> a[0]);
        final Queue<int[]> open = new PriorityQueue<>(comparator);
        final Set<Integer> closed = new HashSet<>();

        gScore.put(start, 0.0);
        initializeStart(graph, start, goal, gScore);
        open.add(new int[]{start});

        while (!open.isEmpty()) {
            final int current = open.poll()[0];
            if (current == goal) {
                return reconstructNodes(graph, parent, goal);
            }
            if (!closed.add(current)) {
                continue;
            }

            expandNode(graph, current, rules, filter, gScore, parent, open, goal);
        }
        return List.of();
    }

    /**
     * Relaxes a single edge.
     *
     * @param from   the current node
     * @param edge   the edge to relax
     * @param graph  the graph to use
     * @param goal   the goal node
     * @param gScore the current gScore
     * @param parent the parent map
     * @param open   the open list
     */
    protected void relax(final int from, final Edge edge, final Graph graph, final int goal,
                         final Map<Integer, Double> gScore,
                         final Map<Integer, Integer> parent,
                         final Queue<int[]> open) {
        final int targetId = edge.target();
        final double tentative = gScore.get(from) + edge.cost();
        if (tentative < gScore.getOrDefault(targetId, Double.POSITIVE_INFINITY)) {
            parent.put(targetId, from);
            gScore.put(targetId, tentative);
            afterRelax(graph, targetId, goal, tentative, gScore);
            open.add(new int[]{targetId});
        }
    }

    /**
     * Reconstructs the path from the goal node to the start node.
     *
     * @param graph  the graph to use
     * @param parent the parent map
     * @param goal   the goal node
     * @return the reconstructed path.
     */
    protected List<Node> reconstructNodes(final Graph graph, final Map<Integer, Integer> parent, final int goal) {
        final List<Node> path = new LinkedList<>();
        Integer current = goal;
        while (current != null) {
            path.addFirst(graph.getNodeById(current));
            current = parent.get(current);
        }
        return path;
    }

    /**
     * Calculates the heuristic distance between two nodes.
     *
     * @param graph      the graph to use
     * @param firstNode  the first node
     * @param secondNode the second node
     * @return the heuristic distance
     */
    protected double heuristic(final Graph graph, final int firstNode, final int secondNode) {
        final Node nodeA = graph.getNodeById(firstNode);
        final Node nodeB = graph.getNodeById(secondNode);
        final double deltaX = nodeA.x() - nodeB.x();
        final double deltaY = nodeA.y() - nodeB.y();
        final double deltaZ = nodeA.z() - nodeB.z();
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
    }

    /**
     * Calculates the priority score for a node.
     *
     * @param nodeId the node id
     * @param gScore the gScore map
     * @return the priority score
     */
    protected double priorityScore(final int nodeId, final Map<Integer, Double> gScore) {
        return gScore.getOrDefault(nodeId, Double.POSITIVE_INFINITY);
    }

    @SuppressWarnings({"PMD.CommentRequired", "PMD.EmptyMethodInAbstractClassShouldBeAbstract"})
    protected void initializeStart(final Graph graph, final int start, final int goal, final Map<Integer, Double> gScore) {
        // Empty
    }

    @SuppressWarnings({"PMD.CommentRequired", "PMD.EmptyMethodInAbstractClassShouldBeAbstract"})
    protected void afterRelax(final Graph graph, final int nodeId, final int goal, final double tentativeG,
                              final Map<Integer, Double> gScore) {
        // Empty
    }

    @SuppressWarnings({"PMD.CommentRequired", "PMD.EmptyMethodInAbstractClassShouldBeAbstract"})
    protected void onExpandNode(final Graph graph, final int nodeId, final TeleportRules rules,
                                final Predicate<Edge> filter, final Map<Integer, Double> gScore,
                                final Map<Integer, Integer> parent, final Queue<int[]> open, final int goal) {
        // Empty
    }

    /**
     * Checks if an edge is allowed.
     *
     * @param graph the graph to use
     * @param edge  the edge to check
     * @param rules the teleport rules to use
     * @return {@code true} if the edge is allowed, {@code false} otherwise
     */
    protected abstract boolean isEdgeAllowed(Graph graph, Edge edge, TeleportRules rules);

    private boolean isMissingNode(final Graph graph, final int start, final int goal) {
        return graph.getNodeById(start) == null || graph.getNodeById(goal) == null;
    }

    private Predicate<Edge> normalizeFilter(final Predicate<Edge> edgeFilter) {
        return edgeFilter == null ? e -> true : edgeFilter;
    }

    private void expandNode(final Graph graph, final int current, final TeleportRules rules, final Predicate<Edge> filter,
                            final Map<Integer, Double> gScore, final Map<Integer, Integer> parent,
                            final Queue<int[]> open, final int goal) {
        for (final Edge edge : graph.neighbors(current)) {
            if (isEdgeAllowed(graph, edge, rules) && filter.test(edge)) {
                relax(current, edge, graph, goal, gScore, parent, open);
            }
        }
        onExpandNode(graph, current, rules, filter, gScore, parent, open, goal);
    }
}
