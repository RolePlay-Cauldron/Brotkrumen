package com.github.roleplaycauldron.brotkrumen.graph.search;

import com.github.roleplaycauldron.brotkrumen.graph.Edge;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.TeleportRules;

import java.util.List;
import java.util.function.Predicate;

/**
 * The interface for the different path algorithms.
 */
public interface PathAlgorithm {
    /**
     * Checks if the algorithm is suitable for the given graph and teleport rules.
     *
     * @param graph the {@link Graph} to search in
     * @param rules the {@link TeleportRules} to use
     * @return {@code true} if the algorithm is suitable, {@code false} otherwise
     */
    boolean suitable(Graph graph, TeleportRules rules);

    /**
     * Finds a path from the start node to the goal node.
     *
     * @param graph         the {@link Graph} to search in
     * @param start         the start node id
     * @param goal          the goal node id
     * @param edgeFilter    the {@link Predicate} to filter the edges to consider
     * @param teleportRules the {@link TeleportRules} to use
     * @return the path as a {@link List} of {@link Node}s
     */
    List<Node> findPath(Graph graph, int start, int goal, Predicate<Edge> edgeFilter, TeleportRules teleportRules);
}
