package com.github.roleplaycauldron.brotkrumen.graph.search.impl;

import com.github.roleplaycauldron.brotkrumen.graph.Edge;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.TeleportRules;
import com.github.roleplaycauldron.brotkrumen.graph.search.PathAlgorithm;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Abstract base class for the different shortest path algorithms.
 */
@SuppressWarnings("PMD.TooManyMethods")
abstract class AbstractShortestPath implements PathAlgorithm {

    /**
     * The default constructor.
     */
    public AbstractShortestPath() {
        // Empty
    }

    @Override
    public List<Node> findPath(final Graph graph, final UUID start, final Set<UUID> goals,
                               final Predicate<Edge> edgeFilter, final TeleportRules rules) {
        if (isMissingNode(graph, start, goals)) {
            return List.of();
        }

        final Predicate<Edge> filter = normalizeFilter(edgeFilter);
        final Map<UUID, Double> gScore = new HashMap<>();
        final Map<UUID, UUID> parent = new HashMap<>();

        final Queue<UUID> open = compareAndGetUUID(gScore);
        final Set<UUID> closed = new HashSet<>();

        gScore.put(start, 0.0);
        initializeStart(graph, start, goals, gScore);
        open.add(start);

        while (!open.isEmpty()) {
            final UUID current = open.poll();
            if (goals.contains(current)) {
                return reconstructNodes(graph, parent, current);
            }
            if (!closed.add(current)) {
                continue;
            }

            expandNode(graph, current, rules, filter, gScore, parent, open, goals);
        }
        return List.of();
    }

    private @NotNull Queue<UUID> compareAndGetUUID(final Map<UUID, Double> gScore) {
        final Comparator<UUID> comparator = (uuidA, uuidB) -> {
            final double psA = priorityScore(uuidA, gScore);
            final double psB = priorityScore(uuidB, gScore);
            final int cmp = Double.compare(psA, psB);
            if (cmp != 0) {
                return cmp;
            }

            return uuidA.compareTo(uuidB);
        };
        return new PriorityQueue<>(comparator);
    }

    /**
     * Relaxes a single edge.
     */
    protected void relax(final UUID from, final Edge edge, final Graph graph, final Set<UUID> goals,
                         final Map<UUID, Double> gScore,
                         final Map<UUID, UUID> parent,
                         final Queue<UUID> open) {
        final UUID targetId = edge.target();
        final double tentative = gScore.get(from) + edge.cost();
        if (tentative < gScore.getOrDefault(targetId, Double.POSITIVE_INFINITY)) {
            parent.put(targetId, from);
            gScore.put(targetId, tentative);
            afterRelax(graph, targetId, goals, tentative, gScore);
            open.add(targetId);
        }
    }

    /**
     * Reconstructs the path from the goal node to the start node.
     */
    protected List<Node> reconstructNodes(final Graph graph, final Map<UUID, UUID> parent, final UUID goal) {
        final List<Node> path = new LinkedList<>();
        UUID current = goal;
        while (current != null) {
            path.addFirst(graph.getNodeById(current));
            current = parent.get(current);
        }
        return path;
    }

    /**
     * Calculates the heuristic distance between two nodes.
     */
    protected double heuristic(final Graph graph, final UUID firstNode, final UUID secondNode) {
        final Node nodeA = graph.getNodeById(firstNode);
        final Node nodeB = graph.getNodeById(secondNode);
        final double deltaX = nodeA.x() - nodeB.x();
        final double deltaY = nodeA.y() - nodeB.y();
        final double deltaZ = nodeA.z() - nodeB.z();
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
    }

    /**
     * Calculates the priority score for a node.
     */
    protected double priorityScore(final UUID nodeId, final Map<UUID, Double> gScore) {
        return gScore.getOrDefault(nodeId, Double.POSITIVE_INFINITY);
    }

    @SuppressWarnings({"PMD.CommentRequired", "PMD.EmptyMethodInAbstractClassShouldBeAbstract"})
    protected void initializeStart(final Graph graph, final UUID start, final Set<UUID> goals,
                                   final Map<UUID, Double> gScore) {
        // Empty – kann von A* usw. überschrieben werden
    }

    @SuppressWarnings({"PMD.CommentRequired", "PMD.EmptyMethodInAbstractClassShouldBeAbstract"})
    protected void afterRelax(final Graph graph, final UUID nodeId, final Set<UUID> goals, final double tentativeG,
                              final Map<UUID, Double> gScore) {
        // Empty
    }

    @SuppressWarnings({"PMD.CommentRequired", "PMD.EmptyMethodInAbstractClassShouldBeAbstract"})
    protected void onExpandNode(final Graph graph, final UUID nodeId, final TeleportRules rules,
                                final Predicate<Edge> filter, final Map<UUID, Double> gScore,
                                final Map<UUID, UUID> parent, final Queue<UUID> open, final Set<UUID> goals) {
        // Empty
    }

    /**
     * Checks if an edge is allowed.
     */
    protected abstract boolean isEdgeAllowed(Graph graph, Edge edge, TeleportRules rules);

    private boolean isMissingNode(final Graph graph, final UUID start, final Set<UUID> goals) {
        return graph.getNodeById(start) == null || goals == null || goals.isEmpty();
    }

    private Predicate<Edge> normalizeFilter(final Predicate<Edge> edgeFilter) {
        return edgeFilter == null ? e -> true : edgeFilter;
    }

    private void expandNode(final Graph graph, final UUID current, final TeleportRules rules,
                            final Predicate<Edge> filter,
                            final Map<UUID, Double> gScore, final Map<UUID, UUID> parent,
                            final Queue<UUID> open, final Set<UUID> goals) {
        for (final Edge edge : graph.neighbors(current)) {
            if (isEdgeAllowed(graph, edge, rules) && filter.test(edge)) {
                relax(current, edge, graph, goals, gScore, parent, open);
            }
        }
        onExpandNode(graph, current, rules, filter, gScore, parent, open, goals);
    }
}
