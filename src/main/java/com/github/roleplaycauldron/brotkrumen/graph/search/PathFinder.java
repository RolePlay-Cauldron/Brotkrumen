package com.github.roleplaycauldron.brotkrumen.graph.search;

import com.github.roleplaycauldron.brotkrumen.graph.Edge;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.graph.TeleportRules;
import com.github.roleplaycauldron.brotkrumen.graph.search.impl.AStarAlgorithm;
import com.github.roleplaycauldron.brotkrumen.graph.search.impl.DijkstraAlgorithm;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The pathfinder class that uses the search registry to find the shortest path.
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
    public List<Node> findPath(final Graph graph, final UUID start, final UUID goal,
                               final Predicate<Edge> edgeFilter, final TeleportRules rules) {
        return findPath(graph, start, Set.of(goal), edgeFilter, rules);
    }

    /**
     * Searches a path from the start node to one of the goal nodes.
     *
     * @param graph      the {@link Graph} to search in
     * @param start      the start node id
     * @param goals      the goal node ids
     * @param edgeFilter the {@link Predicate} to filter the edges to consider
     * @param rules      the {@link TeleportRules} to use
     * @return the path as a {@link List} of {@link Node}s
     */
    public List<Node> findPath(final Graph graph, final UUID start, final Set<UUID> goals,
                               final Predicate<Edge> edgeFilter, final TeleportRules rules) {
        final PathAlgorithm algo = registry.select(graph, rules);
        return algo.findPath(graph, start, goals, edgeFilter, rules);
    }

    /**
     * Searches a path across multiple graphs using inter-graph edges.
     *
     * @param network    graph network containing local graphs and inter-graph edges
     * @param start      start node reference
     * @param goals      goal node references
     * @param edgeFilter edge filter
     * @param rules      teleport rules
     * @return path as node references, empty if no route exists
     */
    public List<NodeRef> findPath(final GraphNetwork network, final NodeRef start, final Collection<NodeRef> goals,
                                  final Predicate<Edge> edgeFilter, final TeleportRules rules) {
        final GraphNetwork.UnifiedGraph unified = network.toUnifiedGraph();
        final UUID unifiedStart = unified.unifiedIdByNodeRef().get(start);
        final Set<UUID> unifiedGoals = goals.stream()
                .map(unified.unifiedIdByNodeRef()::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (unifiedStart == null || unifiedGoals.isEmpty()) {
            return List.of();
        }

        final List<Node> unifiedPath = findPath(unified.graph(), unifiedStart, unifiedGoals, edgeFilter, rules);
        return unifiedPath.stream()
                .map(Node::graphId)
                .map(unified.nodeRefByUnifiedId()::get)
                .toList();
    }

    /**
     * Searches a path across multiple graphs using inter-graph edges.
     *
     * @param network    graph network containing local graphs and inter-graph edges
     * @param start      start node reference
     * @param goal       goal node reference
     * @param edgeFilter edge filter
     * @param rules      teleport rules
     * @return path as node references, empty if no route exists
     */
    public List<NodeRef> findPath(final GraphNetwork network, final NodeRef start, final NodeRef goal,
                                  final Predicate<Edge> edgeFilter, final TeleportRules rules) {
        return findPath(network, start, List.of(goal), edgeFilter, rules);
    }

    /**
     * Searches a path to a target graph. The search ends when any node in the target graph
     * is reached that has an incoming inter-graph edge.
     *
     * @param network       graph network
     * @param start         start node reference
     * @param targetGraphId target graph ID
     * @param edgeFilter    edge filter
     * @param rules         teleport rules
     * @return path as node references
     */
    public List<NodeRef> findPath(final GraphNetwork network, final NodeRef start, final int targetGraphId,
                                  final Predicate<Edge> edgeFilter, final TeleportRules rules) {
        if (start.graphDbId() == targetGraphId) {
            return List.of(start);
        }
        final Set<NodeRef> entryPoints = network.getGraphEntryPoints(targetGraphId);
        return findPath(network, start, entryPoints, edgeFilter, rules);
    }

    /**
     * Searches a path across multiple graphs and resolves the resulting references to concrete nodes.
     *
     * @param network    graph network
     * @param start      start node reference
     * @param goals      goal node references
     * @param edgeFilter edge filter
     * @param rules      teleport rules
     * @return path as concrete nodes
     */
    public List<Node> findNodePath(final GraphNetwork network, final NodeRef start, final Collection<NodeRef> goals,
                                   final Predicate<Edge> edgeFilter, final TeleportRules rules) {
        final List<NodeRef> path = findPath(network, start, goals, edgeFilter, rules);
        return network.resolvePath(path);
    }

    /**
     * Searches a path across multiple graphs and resolves the resulting references to concrete nodes.
     * This method is convenient for visualizers that already work with {@link Node}.
     *
     * @param network    graph network containing local graphs and inter-graph edges
     * @param start      start node reference
     * @param goal       goal node reference
     * @param edgeFilter edge filter
     * @param rules      teleport rules
     * @return path as concrete nodes, empty if no route exists or references cannot be resolved
     */
    public List<Node> findNodePath(final GraphNetwork network, final NodeRef start, final NodeRef goal,
                                   final Predicate<Edge> edgeFilter, final TeleportRules rules) {
        final List<NodeRef> path = findPath(network, start, goal, edgeFilter, rules);
        return network.resolvePath(path);
    }

    /**
     * Searches a path to a target graph and resolves it to concrete nodes.
     *
     * @param network       graph network
     * @param start         start node reference
     * @param targetGraphId target graph ID
     * @param edgeFilter    edge filter
     * @param rules         teleport rules
     * @return path as concrete nodes
     */
    public List<Node> findNodePath(final GraphNetwork network, final NodeRef start, final int targetGraphId,
                                   final Predicate<Edge> edgeFilter, final TeleportRules rules) {
        final List<NodeRef> path = findPath(network, start, targetGraphId, edgeFilter, rules);
        return network.resolvePath(path);
    }
}
