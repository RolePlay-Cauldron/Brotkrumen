package com.github.roleplaycauldron.brotkrumen.command.bk;

import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.graph.TeleportRules;
import com.github.roleplaycauldron.brotkrumen.graph.search.PathFinder;
import com.github.roleplaycauldron.brotkrumen.graph.search.PathResult;
import com.github.roleplaycauldron.brotkrumen.storage.service.GraphNetworkService;
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
@SuppressWarnings({"PMD.CouplingBetweenObjects", "PMD.ShortVariable", "PMD.TooManyMethods"})
public class ResolveService {

    private final GraphService graphService;

    private final GraphNetworkService graphNetworkService;

    private final PathFinder pathFinder;

    /**
     * Creates a resolve service.
     *
     * @param graphService graph service
     */
    public ResolveService(final GraphService graphService) {
        this(graphService, null, new PathFinder());
    }

    /**
     * Creates a resolve service.
     *
     * @param graphService        graph service
     * @param graphNetworkService graph network service
     */
    public ResolveService(final GraphService graphService, final GraphNetworkService graphNetworkService) {
        this(graphService, graphNetworkService, new PathFinder());
    }

    /**
     * Creates a resolve service.
     *
     * @param graphService graph service
     * @param pathFinder   pathfinder
     */
    public ResolveService(final GraphService graphService, final PathFinder pathFinder) {
        this(graphService, null, pathFinder);
    }

    /**
     * Creates a resolve service.
     *
     * @param graphService        graph service
     * @param graphNetworkService graph network service
     * @param pathFinder          pathfinder
     */
    public ResolveService(final GraphService graphService, final GraphNetworkService graphNetworkService,
                          final PathFinder pathFinder) {
        this.graphService = graphService;
        this.graphNetworkService = graphNetworkService;
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
     * Finds the nearest node reference across all supplied graphs.
     *
     * @param graphs   candidate graphs
     * @param location player location snapshot
     * @param radius   maximum distance
     * @return nearest node reference
     */
    public Optional<NodeRef> nearestNodeRef(final Collection<Graph> graphs, final ResolveLocation location,
                                            final double radius) {
        final double maxDistanceSquared = radius * radius;
        return graphs.stream()
                .flatMap(graph -> graph.getNodes().stream()
                        .filter(node -> sameWorld(node, location))
                        .map(node -> new NodeRefDistance(new NodeRef(graph.getGraphId(), node.graphId()),
                                distanceSquared(node, location))))
                .filter(distance -> distance.distanceSquared() <= maxDistanceSquared)
                .min(Comparator.comparingDouble(NodeRefDistance::distanceSquared))
                .map(NodeRefDistance::ref);
    }

    /**
     * Resolves node ids to graph-qualified node references across all graphs.
     *
     * @param nodeIds node ids
     * @return graph-qualified node target resolution
     */
    public NodeRefTargetResolution resolveNodeRefTargets(final Collection<UUID> nodeIds) {
        if (nodeIds == null || nodeIds.isEmpty()) {
            return NodeRefTargetResolution.failure("Please specify at least one node target.");
        }
        final Set<UUID> requested = Set.copyOf(nodeIds);
        final List<NodeRef> refs = graphService.getAllGraphs().stream()
                .flatMap(graph -> graph.getNodes().stream()
                        .filter(node -> requested.contains(node.graphId()))
                        .map(node -> new NodeRef(graph.getGraphId(), node.graphId())))
                .toList();
        if (refs.size() != requested.size()) {
            return NodeRefTargetResolution.failure("No graph contains the requested node targets.");
        }
        return NodeRefTargetResolution.success(refs);
    }

    /**
     * Loads persisted graph networks when the service is available.
     *
     * @return graph networks
     */
    public List<GraphNetwork> loadGraphNetworks() {
        if (graphNetworkService == null) {
            return List.of();
        }
        return List.copyOf(graphNetworkService.loadGraphNetworks());
    }

    /**
     * Finds a graph network containing all required graph ids.
     *
     * @param networks candidate networks
     * @param graphIds required graph ids
     * @return matching network
     */
    public Optional<GraphNetwork> networkContaining(final Collection<GraphNetwork> networks,
                                                    final Collection<Integer> graphIds) {
        final Set<Integer> required = Set.copyOf(graphIds);
        return networks.stream()
                .filter(network -> required.stream().allMatch(network::hasGraph))
                .findFirst();
    }

    /**
     * Wraps one graph as a graph network for graph-local pathfinding.
     *
     * @param graph graph
     * @return single-graph network
     */
    public GraphNetwork singleGraphNetwork(final Graph graph) {
        final GraphNetwork network = new GraphNetwork();
        network.addGraph(graph);
        return network;
    }

    /**
     * Finds a graph-network path to any graph-qualified node goal.
     *
     * @param network graph network
     * @param start   start node reference
     * @param goals   goal node references
     * @return path result
     */
    public PathResult findPath(final GraphNetwork network, final NodeRef start, final Collection<NodeRef> goals) {
        return pathFinder.findPathResult(network, start, goals, null, TeleportRules.disableTeleports());
    }

    /**
     * Finds a graph-network path to any entry point in the target graph.
     *
     * @param network       graph network
     * @param start         start node reference
     * @param targetGraphId target graph database id
     * @return path result
     */
    public PathResult findPath(final GraphNetwork network, final NodeRef start, final int targetGraphId) {
        return pathFinder.findPathResult(network, start, targetGraphId, null, TeleportRules.disableTeleports());
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

    private record NodeRefDistance(NodeRef ref, double distanceSquared) {
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

    /**
     * Resolved graph-qualified node targets.
     *
     * @param nodeRefs graph-qualified node references
     * @param error    error message
     */
    public record NodeRefTargetResolution(List<NodeRef> nodeRefs, String error) {

        /**
         * Creates a successful graph-qualified target resolution.
         *
         * @param nodeRefs graph-qualified node references
         * @return result
         */
        public static NodeRefTargetResolution success(final List<NodeRef> nodeRefs) {
            return new NodeRefTargetResolution(List.copyOf(nodeRefs), null);
        }

        /**
         * Creates a failed graph-qualified target resolution.
         *
         * @param error error message
         * @return result
         */
        public static NodeRefTargetResolution failure(final String error) {
            return new NodeRefTargetResolution(List.of(), error);
        }

        /**
         * Checks if resolution succeeded.
         *
         * @return true when all node references were resolved
         */
        public boolean success() {
            return error == null;
        }
    }
}
