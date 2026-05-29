package com.github.roleplaycauldron.brotkrumen.storage.service;

import com.github.roleplaycauldron.brotkrumen.graph.Graph;

import java.util.Optional;
import java.util.Set;

/**
 * Provides an interface for managing graph-related operations. This service
 * facilitates CRUD operations on graphs, including loading graphs by ID or name,
 * retrieving all stored graphs, saving new graphs, and deleting existing ones.
 */
public interface GraphService {

    /**
     * Loads a graph with the specified ID.
     *
     * @param graphId the unique identifier of the graph to retrieve
     * @return an {@code Optional} containing the graph if found, otherwise an empty {@code Optional}
     */
    Optional<Graph> getGraphById(int graphId);

    /**
     * Loads a graph with the specified name.
     *
     * @param name the name of the graph to retrieve
     * @return an {@code Optional} containing the graph if found, otherwise an empty {@code Optional}
     */
    Optional<Graph> getGraphByName(String name);

    /**
     * Retrieves all stored graphs in the system.
     *
     * @return a set containing all graphs currently managed by the service
     */
    Set<Graph> getAllGraphs();

    /**
     * Saves the provided graph to the system. If a graph with the same unique
     * identifier already exists, it will be updated; otherwise, a new graph
     * will be created and persisted.
     *
     * @param graph the graph object to be saved or updated, containing all relevant
     *              metadata and relationships
     */
    void saveGraph(Graph graph);

    /**
     * Deletes the graph with the specified unique identifier from the storage system.
     * This operation will remove the graph and all its associated nodes and edges.
     *
     * @param graphId the unique identifier of the graph to be deleted
     */
    void deleteGraph(int graphId);

    /**
     * Invalidates the internal cache, forcing a reload of all graph data from storage.
     */
    void invalidateCache();
}
