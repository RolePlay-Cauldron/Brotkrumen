package com.github.roleplaycauldron.brotkrumen.graph.search;

import com.github.roleplaycauldron.brotkrumen.graph.Edge;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.TeleportRules;
import com.github.roleplaycauldron.brotkrumen.graph.search.impl.AStarAlgorithm;
import com.github.roleplaycauldron.brotkrumen.graph.search.impl.DijkstraAlgorithm;

import java.util.List;
import java.util.function.Predicate;

/**
 * The path finder class that uses the search registry to find the shortest path.
 */
public class PathFinder {

    private final SearchRegistry registry;

    /**
     * The default constructor. It automatically registers the {@link AStarAlgorithm} and {@link DijkstraAlgorithm}.
     */
    public PathFinder() {
        this.registry = new SearchRegistry();
        this.registry.register(new AStarAlgorithm());
        this.registry.register(new DijkstraAlgorithm());
    }

    /**
     * The default constructor with a custom {@link SearchRegistry}.
     *
     * @param registry the {@link SearchRegistry} to use
     */
    public PathFinder(final SearchRegistry registry) {
        this.registry = registry;
    }

    /**
     * Searches a path from the start node to the goal node.
     *
     * @param graph      the {@link Graph} to search in
     * @param start      the start node id
     * @param goal       the goal node id
     * @param edgeFilter the {@link Predicate} to filter the edges to consider
     * @param rules      the {@link TeleportRules} to use
     * @return the path as a {@link List} of {@link Node}s
     */
    public List<Node> findPath(final Graph graph, final int start, final int goal, final Predicate<Edge> edgeFilter, final TeleportRules rules) {
        final PathAlgorithm algo = registry.select(graph, rules);
        return algo.findPath(graph, start, goal, edgeFilter, rules);
    }
}
