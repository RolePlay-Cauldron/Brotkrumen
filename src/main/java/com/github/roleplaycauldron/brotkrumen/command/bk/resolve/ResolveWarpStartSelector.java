package com.github.roleplaycauldron.brotkrumen.command.bk.resolve;

import com.github.roleplaycauldron.brotkrumen.graph.Edge;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork;
import com.github.roleplaycauldron.brotkrumen.graph.InterGraphEdge;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Selects warp starts for resolve when no nearby node exists.
 */
@SuppressWarnings("PMD.TooManyMethods")
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
    /* default */ Optional<Candidate> selectGraph(final ResolveOptions options, final int targetGraphId,
                                                 final TeleportRules rules) {
        if (!canUseWarpFallback(options, rules)) {
            return Optional.empty();
        }
        final List<GraphNetwork> networks = resolveService.loadGraphNetworks();
        return warpStartRefs(rules).stream()
                .map(start -> graphCandidate(start, targetGraphId, networks, rules))
                .filter(Objects::nonNull)
                .min(Comparator.comparingDouble(Candidate::cost));
    }

    /**
     * Selects the cheapest warp start route to target nodes.
     */
    /* default */ Optional<Candidate> selectNodes(final ResolveOptions options, final Collection<NodeRef> goals,
                                                  final TeleportRules rules) {
        if (!canUseWarpFallback(options, rules)) {
            return Optional.empty();
        }
        final List<GraphNetwork> networks = resolveService.loadGraphNetworks();
        return warpStartRefs(rules).stream()
                .map(start -> nodeCandidate(start, goals, networks, rules))
                .filter(Objects::nonNull)
                .min(Comparator.comparingDouble(Candidate::cost));
    }

    private boolean canUseWarpFallback(final ResolveOptions options, final TeleportRules rules) {
        return options.autoTeleportOptions().startFromWarpWhenNoNearbyNode() && rules.isWarpingEnabled();
    }

    private List<NodeRef> warpStartRefs(final TeleportRules rules) {
        return rules.getWarps().stream()
                .filter(Warp::enabled)
                .flatMap(warp -> graphService.getAllGraphs().stream()
                        .flatMap(graph -> graph.getNodes().stream()
                                .filter(node -> node.graphId().equals(warp.targetNodeId()))
                                .map(node -> new NodeRef(graph.getGraphId(), node.graphId()))))
                .toList();
    }

    private Candidate graphCandidate(final NodeRef start, final int targetGraphId,
                                     final Collection<GraphNetwork> networks, final TeleportRules rules) {
        final GraphNetwork network = networkForStartAndGraph(start, targetGraphId, networks).orElse(null);
        if (network == null) {
            return null;
        }
        final PathResult path = resolveService.findPath(network, start, targetGraphId, rules);
        return path.nodes().isEmpty() || startsWithWarp(path) ? null
                : new Candidate(start, network, path, routeCost(network, path, rules));
    }

    private Candidate nodeCandidate(final NodeRef start, final Collection<NodeRef> goals,
                                    final Collection<GraphNetwork> networks, final TeleportRules rules) {
        final GraphNetwork network = networkForStartAndGoals(start, goals, networks).orElse(null);
        if (network == null) {
            return null;
        }
        final PathResult path = resolveService.findPath(network, start, goals, rules);
        return path.nodes().isEmpty() || startsWithWarp(path) ? null
                : new Candidate(start, network, path, routeCost(network, path, rules));
    }

    private boolean startsWithWarp(final PathResult path) {
        return !path.segments().isEmpty() && path.segments().getFirst().traversalKind() == TraversalKind.WARP;
    }

    private Optional<GraphNetwork> networkForStartAndGraph(final NodeRef start, final int targetGraphId,
                                                          final Collection<GraphNetwork> networks) {
        if (start.graphDbId() == targetGraphId) {
            return graphService.getGraphById(start.graphDbId()).map(resolveService::singleGraphNetwork);
        }
        return resolveService.networkContaining(networks, List.of(start.graphDbId(), targetGraphId));
    }

    private Optional<GraphNetwork> networkForStartAndGoals(final NodeRef start, final Collection<NodeRef> goals,
                                                          final Collection<GraphNetwork> networks) {
        final Collection<Integer> requiredGraphIds = Stream.concat(Stream.of(start.graphDbId()),
                        goals.stream().map(NodeRef::graphDbId))
                .collect(Collectors.toSet());
        if (requiredGraphIds.size() == SINGLE_GRAPH_COUNT) {
            return graphService.getGraphById(start.graphDbId()).map(resolveService::singleGraphNetwork);
        }
        return resolveService.networkContaining(networks, requiredGraphIds);
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
}
