package com.github.roleplaycauldron.brotkrumen.graph.search;

import com.github.roleplaycauldron.brotkrumen.api.graph.search.PathSearchService;
import com.github.roleplaycauldron.brotkrumen.graph.Edge;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.graph.TeleportRules;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Predicate;

/**
 * Executor-backed async path search service.
 */
public class PathSearchServiceImpl implements PathSearchService {

    private final PathFinder pathFinder;

    private final Executor executor;

    /**
     * Creates an async path search service.
     *
     * @param pathFinder path finder
     * @param executor   search executor
     */
    public PathSearchServiceImpl(final PathFinder pathFinder, final Executor executor) {
        this.pathFinder = pathFinder;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<PathResult> findPath(final Graph graph, final UUID start, final UUID goal,
                                                  final Predicate<Edge> edgeFilter, final TeleportRules rules) {
        return CompletableFuture.supplyAsync(() -> pathFinder.findPathResult(graph, start, goal, edgeFilter, rules),
                executor);
    }

    @Override
    public CompletableFuture<PathResult> findPath(final Graph graph, final UUID start, final Set<UUID> goals,
                                                  final Predicate<Edge> edgeFilter, final TeleportRules rules) {
        return CompletableFuture.supplyAsync(() -> pathFinder.findPathResult(graph, start, goals, edgeFilter, rules),
                executor);
    }

    @Override
    public CompletableFuture<PathResult> findPath(final GraphNetwork network, final NodeRef start,
                                                  final Collection<NodeRef> goals,
                                                  final Predicate<Edge> edgeFilter, final TeleportRules rules) {
        return CompletableFuture.supplyAsync(() -> pathFinder.findPathResult(network, start, goals, edgeFilter, rules),
                executor);
    }

    @Override
    public CompletableFuture<PathResult> findPath(final GraphNetwork network, final NodeRef start, final NodeRef goal,
                                                  final Predicate<Edge> edgeFilter, final TeleportRules rules) {
        return CompletableFuture.supplyAsync(() -> pathFinder.findPathResult(network, start, goal, edgeFilter, rules),
                executor);
    }

    @Override
    public CompletableFuture<PathResult> findPathToGraph(final GraphNetwork network, final NodeRef start,
                                                         final int targetGraphId, final Predicate<Edge> edgeFilter,
                                                         final TeleportRules rules) {
        return CompletableFuture.supplyAsync(() -> pathFinder.findPathResult(network, start, targetGraphId,
                edgeFilter, rules), executor);
    }
}
