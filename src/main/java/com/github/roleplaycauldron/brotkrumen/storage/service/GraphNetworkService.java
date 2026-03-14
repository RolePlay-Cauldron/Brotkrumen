package com.github.roleplaycauldron.brotkrumen.storage.service;

import com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork;

import java.util.Collection;

/**
 * Service for managing GraphNetwork operations.
 */
public interface GraphNetworkService {

    /**
     * Loads all graphs and inter-graph connections and groups them into multiple
     * networks based on their connectivity.
     *
     * @return collection of graph networks
     */
    Collection<GraphNetwork> loadGraphNetworks();

    /**
     * Persists all inter-graph edges for the provided network.
     * Graphs must already be persisted.
     *
     * @param network network containing inter-graph edges to persist
     */
    void saveInterGraphEdges(GraphNetwork network);

    /**
     * Deletes the inter-graph edges for the provided network.
     *
     * @param network network containing inter-graph edges to delete
     */
    void deleteInterGraphEdges(GraphNetwork network);
}
