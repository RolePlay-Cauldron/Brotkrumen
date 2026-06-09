package com.github.roleplaycauldron.brotkrumen.api.service;

import com.github.roleplaycauldron.brotkrumen.graph.Graph;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Async public service for persisted graph operations.
 */
public interface GraphService {

    /**
     * Loads a graph by database id.
     *
     * @param graphId graph database id
     * @return future graph lookup
     */
    CompletableFuture<Optional<Graph>> graphById(int graphId);

    /**
     * Loads a graph by name.
     *
     * @param name graph name
     * @return future graph lookup
     */
    CompletableFuture<Optional<Graph>> graphByName(String name);

    /**
     * Loads all graphs.
     *
     * @return future graph set
     */
    CompletableFuture<Set<Graph>> allGraphs();

    /**
     * Saves a graph.
     *
     * @param graph graph to persist
     * @return completion future
     */
    CompletableFuture<Void> saveGraph(Graph graph);

    /**
     * Deletes a graph by database id.
     *
     * @param graphId graph database id
     * @return completion future
     */
    CompletableFuture<Void> deleteGraph(int graphId);

    /**
     * Reloads graph cache from storage.
     *
     * @return completion future
     */
    CompletableFuture<Void> reloadGraphs();
}
