package com.github.roleplaycauldron.brotkrumen.api.service;

import com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork;
import com.github.roleplaycauldron.brotkrumen.graph.InterGraphEdge;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Async public service for persisted graph network operations.
 */
public interface GraphNetworkService {

    /**
     * Loads graph networks.
     *
     * @return future graph networks
     */
    CompletableFuture<Collection<GraphNetwork>> graphNetworks();

    /**
     * Saves all inter-graph edges for a network.
     *
     * @param network graph network
     * @return completion future
     */
    CompletableFuture<Void> saveInterGraphEdges(GraphNetwork network);

    /**
     * Loads inter-graph edges matching graph ids.
     *
     * @param graphIds graph database ids
     * @return future edge set
     */
    CompletableFuture<Set<InterGraphEdge>> interGraphEdges(Collection<Integer> graphIds);

    /**
     * Saves inter-graph edges.
     *
     * @param edges edges to persist
     * @return completion future
     */
    CompletableFuture<Void> saveInterGraphEdges(Collection<InterGraphEdge> edges);

    /**
     * Deletes inter-graph edges for a graph.
     *
     * @param graphId graph database id
     * @return future deleted row count
     */
    CompletableFuture<Integer> deleteInterGraphEdgesForGraph(int graphId);

    /**
     * Deletes inter-graph edges from a network.
     *
     * @param network graph network
     * @return completion future
     */
    CompletableFuture<Void> deleteInterGraphEdges(GraphNetwork network);

    /**
     * Reloads graph networks.
     *
     * @return future graph networks
     */
    CompletableFuture<Collection<GraphNetwork>> reloadGraphNetworks();
}
