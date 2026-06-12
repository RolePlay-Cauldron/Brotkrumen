package com.github.roleplaycauldron.brotkrumen.api.graph.search;

import com.github.roleplaycauldron.brotkrumen.graph.Edge;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.graph.TeleportRules;
import com.github.roleplaycauldron.brotkrumen.graph.search.PathResult;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/**
 * Async public service for path searches.
 */
public interface PathSearchService {

    /**
     * Finds a graph-local path to one goal.
     *
     * @param graph      graph to search
     * @param start      start node id
     * @param goal       goal node id
     * @param edgeFilter edge filter
     * @param rules      teleport rules
     * @return future path result
     */
    CompletableFuture<PathResult> findPath(Graph graph, UUID start, UUID goal, Predicate<Edge> edgeFilter,
                                           TeleportRules rules);

    /**
     * Finds a graph-local path to any goal.
     *
     * @param graph      graph to search
     * @param start      start node id
     * @param goals      goal node ids
     * @param edgeFilter edge filter
     * @param rules      teleport rules
     * @return future path result
     */
    CompletableFuture<PathResult> findPath(Graph graph, UUID start, Set<UUID> goals, Predicate<Edge> edgeFilter,
                                           TeleportRules rules);

    /**
     * Finds a network path to any goal.
     *
     * @param network    graph network
     * @param start      start node reference
     * @param goals      goal node references
     * @param edgeFilter edge filter
     * @param rules      teleport rules
     * @return future path result
     */
    CompletableFuture<PathResult> findPath(GraphNetwork network, NodeRef start, Collection<NodeRef> goals,
                                           Predicate<Edge> edgeFilter, TeleportRules rules);

    /**
     * Finds a network path to one goal.
     *
     * @param network    graph network
     * @param start      start node reference
     * @param goal       goal node reference
     * @param edgeFilter edge filter
     * @param rules      teleport rules
     * @return future path result
     */
    CompletableFuture<PathResult> findPath(GraphNetwork network, NodeRef start, NodeRef goal,
                                           Predicate<Edge> edgeFilter, TeleportRules rules);

    /**
     * Finds a network path to a graph.
     *
     * @param network       graph network
     * @param start         start node reference
     * @param targetGraphId target graph database id
     * @param edgeFilter    edge filter
     * @param rules         teleport rules
     * @return future path result
     */
    CompletableFuture<PathResult> findPathToGraph(GraphNetwork network, NodeRef start, int targetGraphId,
                                                  Predicate<Edge> edgeFilter, TeleportRules rules);
}
