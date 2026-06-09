package com.github.roleplaycauldron.brotkrumen.storage.service;

import com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork;
import com.github.roleplaycauldron.brotkrumen.graph.InterGraphEdge;

import java.util.Collection;
import java.util.Set;

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
     * Loads inter-graph edges whose source or target graph is in the requested set.
     *
     * @param graphIds graph database ids
     * @return matching inter-graph edge records
     */
    Set<InterGraphEdge> loadInterGraphEdges(Collection<Integer> graphIds);

    /**
     * Creates or updates the provided inter-graph edge records.
     *
     * @param edges edge records to persist
     */
    void saveInterGraphEdges(Collection<InterGraphEdge> edges);

    /**
     * Deletes all inter-graph edges whose source or target graph is the requested graph.
     *
     * @param graphId graph database id
     * @return number of deleted rows
     */
    int deleteInterGraphEdgesForGraph(int graphId);

    /**
     * Deletes the inter-graph edges for the provided network.
     *
     * @param network network containing inter-graph edges to delete
     */
    void deleteInterGraphEdges(GraphNetwork network);

    /**
     * Invalidates the internal graph-network cache, reloads all graphs and inter-graph edges
     * from the database, and rebuilds the cached graph networks.
     *
     * @return collection of reloaded graph networks
     */
    Collection<GraphNetwork> reloadGraphNetworks();
}
