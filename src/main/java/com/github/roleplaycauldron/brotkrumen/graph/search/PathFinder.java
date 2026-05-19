package com.github.roleplaycauldron.brotkrumen.graph.search;

import com.github.roleplaycauldron.brotkrumen.graph.Edge;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.graph.TeleportRules;
import com.github.roleplaycauldron.brotkrumen.graph.Warp;
import com.github.roleplaycauldron.brotkrumen.graph.search.impl.AStarAlgorithm;
import com.github.roleplaycauldron.brotkrumen.graph.search.impl.DijkstraAlgorithm;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The pathfinder class that uses the search registry to find the shortest path.
 */
@SuppressWarnings("PMD.TooManyMethods")
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
     * @return the path as node references
     */
    public List<NodeRef> findPath(final Graph graph, final UUID start, final UUID goal,
                                  final Predicate<Edge> edgeFilter, final TeleportRules rules) {
        return findPathResult(graph, start, goal, edgeFilter, rules).nodes();
    }

    /**
     * Searches a path from the start node to one of the goal nodes.
     *
     * @param graph      the {@link Graph} to search in
     * @param start      the start node id
     * @param goals      the goal node ids
     * @param edgeFilter the {@link Predicate} to filter the edges to consider
     * @param rules      the {@link TeleportRules} to use
     * @return the path as node references
     */
    public List<NodeRef> findPath(final Graph graph, final UUID start, final Set<UUID> goals,
                                  final Predicate<Edge> edgeFilter, final TeleportRules rules) {
        return findPathResult(graph, start, goals, edgeFilter, rules).nodes();
    }

    /**
     * Searches a structured path result from the start node to the goal node.
     *
     * @param graph      the {@link Graph} to search in
     * @param start      the start node id
     * @param goal       the goal node id
     * @param edgeFilter the {@link Predicate} to filter the edges to consider
     * @param rules      the {@link TeleportRules} to use
     * @return structured path result
     */
    public PathResult findPathResult(final Graph graph, final UUID start, final UUID goal,
                                     final Predicate<Edge> edgeFilter, final TeleportRules rules) {
        return findPathResult(graph, start, Set.of(goal), edgeFilter, rules);
    }

    /**
     * Searches a structured path result from the start node to one of the goal nodes.
     *
     * @param graph      the {@link Graph} to search in
     * @param start      the start node id
     * @param goals      the goal node ids
     * @param edgeFilter the {@link Predicate} to filter the edges to consider
     * @param rules      the {@link TeleportRules} to use
     * @return structured path result
     */
    public PathResult findPathResult(final Graph graph, final UUID start, final Set<UUID> goals,
                                     final Predicate<Edge> edgeFilter, final TeleportRules rules) {
        final PathAlgorithm algo = registry.select(graph, rules);
        return algo.findPathResult(graph, start, goals, edgeFilter, rules);
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
        return findPathResult(network, start, goals, edgeFilter, rules).nodes();
    }

    /**
     * Searches a structured path result across multiple graphs using inter-graph edges.
     *
     * @param network    graph network containing local graphs and inter-graph edges
     * @param start      start node reference
     * @param goals      goal node references
     * @param edgeFilter edge filter
     * @param rules      teleport rules
     * @return structured path result, empty if no route exists
     */
    public PathResult findPathResult(final GraphNetwork network, final NodeRef start, final Collection<NodeRef> goals,
                                     final Predicate<Edge> edgeFilter, final TeleportRules rules) {
        final GraphNetwork.UnifiedGraph unified = network.toUnifiedGraph(rules);
        final UUID unifiedStart = unified.unifiedIdByNodeRef().get(start);
        final Set<UUID> unifiedGoals = goals.stream()
                .map(unified.unifiedIdByNodeRef()::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (unifiedStart == null || unifiedGoals.isEmpty()) {
            return PathResult.empty();
        }

        final PathResult unifiedPath = findPathResult(unified.graph(), unifiedStart, unifiedGoals, edgeFilter,
                translateWarpRules(rules, unified));
        return mapUnifiedResult(unifiedPath, unified);
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
     * Searches a structured path result across multiple graphs using inter-graph edges.
     *
     * @param network    graph network containing local graphs and inter-graph edges
     * @param start      start node reference
     * @param goal       goal node reference
     * @param edgeFilter edge filter
     * @param rules      teleport rules
     * @return structured path result, empty if no route exists
     */
    public PathResult findPathResult(final GraphNetwork network, final NodeRef start, final NodeRef goal,
                                     final Predicate<Edge> edgeFilter, final TeleportRules rules) {
        return findPathResult(network, start, List.of(goal), edgeFilter, rules);
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

    private TeleportRules translateWarpRules(final TeleportRules rules, final GraphNetwork.UnifiedGraph unified) {
        if (rules == null || rules.getWarps().isEmpty()) {
            return rules;
        }
        final List<Warp> translatedWarps = rules.getWarps().stream()
                .map(warp -> translateWarp(warp, unified))
                .filter(Objects::nonNull)
                .toList();
        return new TeleportRules(rules.isLocalTeleportEnabled(), rules.isInterGraphTeleportEnabled(),
                rules.isWarpingEnabled(), translatedWarps);
    }

    private Warp translateWarp(final Warp warp, final GraphNetwork.UnifiedGraph unified) {
        return unified.unifiedIdByNodeRef().entrySet().stream()
                .filter(entry -> entry.getKey().nodeId().equals(warp.targetNodeId()))
                .findFirst()
                .map(Map.Entry::getValue)
                .map(target -> new Warp(warp.key(), target, warp.cost(), warp.enabled(), warp.needPermission()))
                .orElse(null);
    }

    private PathResult mapUnifiedResult(final PathResult result, final GraphNetwork.UnifiedGraph unified) {
        if (result.nodes().isEmpty()) {
            return PathResult.empty();
        }
        final List<NodeRef> nodes = result.nodes().stream()
                .map(NodeRef::nodeId)
                .map(unified.nodeRefByUnifiedId()::get)
                .filter(Objects::nonNull)
                .toList();
        final List<PathSegment> segments = result.segments().stream()
                .map(segment -> mapUnifiedSegment(segment, unified))
                .filter(Objects::nonNull)
                .toList();
        return new PathResult(nodes, segments);
    }

    private PathSegment mapUnifiedSegment(final PathSegment segment, final GraphNetwork.UnifiedGraph unified) {
        final NodeRef source = unified.nodeRefByUnifiedId().get(segment.source().nodeId());
        final NodeRef target = unified.nodeRefByUnifiedId().get(segment.target().nodeId());
        if (source == null || target == null) {
            return null;
        }
        return new PathSegment(source, target, segment.traversalKind(), segment.edgeId(), segment.warpKey());
    }

}
