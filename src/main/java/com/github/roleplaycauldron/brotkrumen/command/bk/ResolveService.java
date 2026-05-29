package com.github.roleplaycauldron.brotkrumen.command.bk;

import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.TeleportRules;
import com.github.roleplaycauldron.brotkrumen.graph.search.PathFinder;
import com.github.roleplaycauldron.brotkrumen.graph.search.PathResult;
import com.github.roleplaycauldron.brotkrumen.storage.service.GraphService;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Graph-local resolve operations used by `/bk resolve`.
 */
@SuppressWarnings("PMD.ShortVariable")
public class ResolveService {

    private final GraphService graphService;

    private final PathFinder pathFinder;

    /**
     * Creates a resolve service.
     *
     * @param graphService graph service
     */
    public ResolveService(final GraphService graphService) {
        this(graphService, new PathFinder());
    }

    /**
     * Creates a resolve service.
     *
     * @param graphService graph service
     * @param pathFinder   pathfinder
     */
    public ResolveService(final GraphService graphService, final PathFinder pathFinder) {
        this.graphService = graphService;
        this.pathFinder = pathFinder;
    }

    /**
     * Resolves a graph by name or id.
     *
     * @param graphKey graph name or id
     * @return graph
     */
    public Optional<Graph> resolveGraph(final String graphKey) {
        if (graphKey == null || graphKey.isBlank()) {
            return Optional.empty();
        }
        final Optional<Graph> byName = graphService.getGraphByName(graphKey);
        if (byName.isPresent()) {
            return byName;
        }
        try {
            return graphService.getGraphById(Integer.parseInt(graphKey));
        } catch (final NumberFormatException ex) {
            return Optional.empty();
        }
    }

    /**
     * Finds the nearest node in a graph.
     *
     * @param graph    graph
     * @param location player location snapshot
     * @param radius   maximum distance
     * @return nearest node
     */
    public Optional<Node> nearestNode(final Graph graph, final ResolveLocation location, final double radius) {
        final double maxDistanceSquared = radius * radius;
        return graph.getNodes().stream()
                .filter(node -> sameWorld(node, location))
                .map(node -> new NodeDistance(node, distanceSquared(node, location)))
                .filter(distance -> distance.distanceSquared() <= maxDistanceSquared)
                .min(Comparator.comparingDouble(NodeDistance::distanceSquared))
                .map(NodeDistance::node);
    }

    /**
     * Resolves all node ids to one shared graph.
     *
     * @param nodeIds node ids
     * @return graph containing all node ids
     */
    public NodeTargetResolution resolveNodeTargets(final Collection<UUID> nodeIds) {
        if (nodeIds == null || nodeIds.isEmpty()) {
            return NodeTargetResolution.failure("Please specify at least one node target.");
        }
        final Set<UUID> requested = Set.copyOf(nodeIds);
        Graph containingGraph = null;
        for (final Graph graph : graphService.getAllGraphs()) {
            final Set<UUID> present = graph.getNodes().stream()
                    .map(Node::graphId)
                    .filter(requested::contains)
                    .collect(Collectors.toSet());
            if (present.isEmpty()) {
                continue;
            }
            if (containingGraph != null) {
                return NodeTargetResolution.failure("Node targets must be in the same graph.");
            }
            containingGraph = graph;
            if (present.size() != requested.size()) {
                return NodeTargetResolution.failure("All node targets must exist in one graph.");
            }
        }
        if (containingGraph == null) {
            return NodeTargetResolution.failure("No graph contains the requested node targets.");
        }
        return NodeTargetResolution.success(containingGraph, List.copyOf(requested));
    }

    /**
     * Finds a graph-local path.
     *
     * @param graph graph
     * @param start start node
     * @param goals goal node ids
     * @return path result
     */
    public PathResult findPath(final Graph graph, final UUID start, final Set<UUID> goals) {
        return pathFinder.findPathResult(graph, start, goals, null, TeleportRules.disableTeleports());
    }

    private boolean sameWorld(final Node node, final ResolveLocation location) {
        return Objects.equals(node.worldId(), location.worldId());
    }

    private double distanceSquared(final Node node, final ResolveLocation location) {
        final double dx = node.x() - location.x();
        final double dy = node.y() - location.y();
        final double dz = node.z() - location.z();
        return dx * dx + dy * dy + dz * dz;
    }

    private record NodeDistance(Node node, double distanceSquared) {
    }

    /**
     * Resolved node targets.
     *
     * @param graph   graph containing all targets
     * @param nodeIds node ids
     * @param error   error message
     */
    public record NodeTargetResolution(Graph graph, List<UUID> nodeIds, String error) {

        /**
         * Creates a successful resolution.
         *
         * @param graph   graph
         * @param nodeIds node ids
         * @return result
         */
        public static NodeTargetResolution success(final Graph graph, final List<UUID> nodeIds) {
            return new NodeTargetResolution(graph, List.copyOf(nodeIds), null);
        }

        /**
         * Creates a failed resolution.
         *
         * @param error error message
         * @return result
         */
        public static NodeTargetResolution failure(final String error) {
            return new NodeTargetResolution(null, List.of(), error);
        }

        /**
         * Checks if resolution succeeded.
         *
         * @return true when a graph was resolved
         */
        public boolean success() {
            return graph != null;
        }
    }
}
