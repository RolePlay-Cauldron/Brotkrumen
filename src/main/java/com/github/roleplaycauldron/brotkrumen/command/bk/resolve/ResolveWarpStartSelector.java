package com.github.roleplaycauldron.brotkrumen.command.bk.resolve;

import com.github.roleplaycauldron.brotkrumen.graph.Edge;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork;
import com.github.roleplaycauldron.brotkrumen.graph.InterGraphEdge;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.graph.TeleportRules;
import com.github.roleplaycauldron.brotkrumen.graph.Warp;
import com.github.roleplaycauldron.brotkrumen.graph.search.PathResult;
import com.github.roleplaycauldron.brotkrumen.graph.search.PathSegment;
import com.github.roleplaycauldron.brotkrumen.graph.search.TraversalKind;
import com.github.roleplaycauldron.brotkrumen.storage.service.GraphService;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Selects warp starts for resolve when no nearby node exists.
 */
final class ResolveWarpStartSelector {

    private static final int SINGLE_GRAPH_COUNT = 1;

    private final GraphService graphService;

    private final ResolveService resolveService;

    /**
     * Creates a selector.
     *
     * @param graphService   graph service
     * @param resolveService resolve service
     */
    /* default */ ResolveWarpStartSelector(final GraphService graphService, final ResolveService resolveService) {
        this.graphService = Objects.requireNonNull(graphService, "graphService");
        this.resolveService = Objects.requireNonNull(resolveService, "resolveService");
    }

    /**
     * Selects the cheapest warp start route to a target graph.
     */
    /* default */ Optional<Candidate> selectGraph(final ResolveOptions options, final ResolveLocation location,
                                                  final int targetGraphId, final TeleportRules rules) {
        if (!canUseWarpFallback(options, rules)) {
            return Optional.empty();
        }
        final List<GraphNetwork> networks = resolveService.loadGraphNetworks();
        final Set<Integer> requiredGraphIds = Set.of(targetGraphId);
        return startNetworks(requiredGraphIds, networks).stream()
                .map(network -> withTemporaryStart(network, location, requiredGraphIds))
                .map(startNetwork -> graphCandidate(startNetwork, targetGraphId, rules))
                .filter(Objects::nonNull)
                .min(Comparator.comparingDouble(Candidate::cost));
    }

    /**
     * Selects the cheapest warp start route to target nodes.
     */
    /* default */ Optional<Candidate> selectNodes(final ResolveOptions options, final ResolveLocation location,
                                                  final Collection<NodeRef> goals, final TeleportRules rules) {
        if (!canUseWarpFallback(options, rules)) {
            return Optional.empty();
        }
        final List<GraphNetwork> networks = resolveService.loadGraphNetworks();
        final Set<Integer> requiredGraphIds = goals.stream().map(NodeRef::graphDbId).collect(Collectors.toSet());
        return startNetworks(requiredGraphIds, networks).stream()
                .map(network -> withTemporaryStart(network, location, requiredGraphIds))
                .map(startNetwork -> nodeCandidate(startNetwork, goals, rules))
                .filter(Objects::nonNull)
                .min(Comparator.comparingDouble(Candidate::cost));
    }

    private boolean canUseWarpFallback(final ResolveOptions options, final TeleportRules rules) {
        return options.autoTeleportOptions().startFromWarpWhenNoNearbyNode() && rules.isWarpingEnabled();
    }

    private List<GraphNetwork> startNetworks(final Set<Integer> requiredGraphIds,
                                             final Collection<GraphNetwork> networks) {
        if (requiredGraphIds.size() == SINGLE_GRAPH_COUNT) {
            final int requiredGraphId = requiredGraphIds.iterator().next();
            final List<GraphNetwork> graphNetworks = networks.stream()
                    .filter(network -> network.hasGraph(requiredGraphId))
                    .toList();
            return Stream.concat(graphService.getGraphById(requiredGraphId)
                            .map(resolveService::singleGraphNetwork)
                            .stream(), graphNetworks.stream())
                    .toList();
        }
        return networks.stream()
                .filter(network -> requiredGraphIds.stream().allMatch(network::hasGraph))
                .toList();
    }

    private StartNetwork withTemporaryStart(final GraphNetwork network, final ResolveLocation location,
                                            final Set<Integer> preferredGraphIds) {
        final int startGraphId = preferredGraphIds.stream()
                .filter(network::hasGraph)
                .findFirst()
                .orElseGet(() -> network.getGraphs().iterator().next().getGraphId());
        final GraphNetwork copy = copyNetwork(network);
        final Graph startGraph = copy.getGraph(startGraphId);
        final UUID startNodeId = UUID.randomUUID();
        startGraph.addNode(new Node(startNodeId, location.x(), location.y(), location.z(), location.worldId()));
        return new StartNetwork(new NodeRef(startGraphId, startNodeId), copy);
    }

    private GraphNetwork copyNetwork(final GraphNetwork network) {
        final GraphNetwork copy = new GraphNetwork();
        for (final Graph graph : network.getGraphs()) {
            copy.addGraph(graph.copy());
        }
        for (final InterGraphEdge edge : network.getInterGraphEdges()) {
            copy.addInterGraphEdge(edge);
        }
        return copy;
    }

    private Candidate graphCandidate(final StartNetwork startNetwork, final int targetGraphId,
                                     final TeleportRules rules) {
        final List<NodeRef> goals = startNetwork.network().getGraph(targetGraphId).getNodes().stream()
                .map(node -> new NodeRef(targetGraphId, node.graphId()))
                .filter(ref -> !ref.equals(startNetwork.start()))
                .toList();
        if (goals.isEmpty()) {
            return null;
        }
        final PathResult path = resolveService.findPath(startNetwork.network(), startNetwork.start(), goals, rules);
        return path.nodes().isEmpty() ? null
                : new Candidate(startNetwork.start(), startNetwork.network(), path,
                routeCost(startNetwork.network(), path, rules));
    }

    private Candidate nodeCandidate(final StartNetwork startNetwork, final Collection<NodeRef> goals,
                                    final TeleportRules rules) {
        final PathResult path = resolveService.findPath(startNetwork.network(), startNetwork.start(), goals, rules);
        return path.nodes().isEmpty() ? null
                : new Candidate(startNetwork.start(), startNetwork.network(), path,
                routeCost(startNetwork.network(), path, rules));
    }

    private double routeCost(final GraphNetwork network, final PathResult path, final TeleportRules rules) {
        double result = 0.0D;
        for (final PathSegment segment : path.segments()) {
            result += segmentCost(network, segment, rules);
        }
        return result;
    }

    private double segmentCost(final GraphNetwork network, final PathSegment segment, final TeleportRules rules) {
        if (segment.traversalKind() == TraversalKind.WARP) {
            return rules.getWarp(segment.warpKey()).map(Warp::cost).orElse(Double.POSITIVE_INFINITY);
        }
        if (segment.traversalKind() == TraversalKind.INTERGRAPH_NORMAL
                || segment.traversalKind() == TraversalKind.INTERGRAPH_TELEPORT) {
            return network.getInterGraphEdges().stream()
                    .filter(edge -> edge.enabled() && edge.source().equals(segment.source())
                            && edge.target().equals(segment.target()))
                    .mapToDouble(InterGraphEdge::cost)
                    .min().orElse(Double.POSITIVE_INFINITY);
        }
        final Graph graph = network.getGraph(segment.source().graphDbId());
        if (graph == null) {
            return Double.POSITIVE_INFINITY;
        }
        return graph.neighbors(segment.source().nodeId()).stream()
                .filter(edge -> edge.target().equals(segment.target().nodeId()))
                .mapToDouble(Edge::cost)
                .min().orElse(Double.POSITIVE_INFINITY);
    }

    /**
     * Selected warp-start route.
     *
     * @param start   warp target start
     * @param network route network
     * @param path    route path
     * @param cost    total route cost
     */
    /* default */ record Candidate(NodeRef start, GraphNetwork network, PathResult path, double cost) {
    }

    private record StartNetwork(NodeRef start, GraphNetwork network) {
    }
}
