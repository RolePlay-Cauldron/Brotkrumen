package com.github.roleplaycauldron.brotkrumen.storage.service;

import com.github.roleplaycauldron.brotkrumen.graph.Graph;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Executor-backed public async graph service adapter.
 */
public class AsyncGraphService implements com.github.roleplaycauldron.brotkrumen.api.service.GraphService {

    private final GraphService delegate;

    private final Executor executor;

    /**
     * Creates an async graph service adapter.
     *
     * @param delegate synchronous delegate
     * @param executor database executor
     */
    public AsyncGraphService(final GraphService delegate, final Executor executor) {
        this.delegate = delegate;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<Optional<Graph>> graphById(final int graphId) {
        return CompletableFuture.supplyAsync(() -> delegate.getGraphById(graphId), executor);
    }

    @Override
    public CompletableFuture<Optional<Graph>> graphByName(final String name) {
        return CompletableFuture.supplyAsync(() -> delegate.getGraphByName(name), executor);
    }

    @Override
    public CompletableFuture<Set<Graph>> allGraphs() {
        return CompletableFuture.supplyAsync(delegate::getAllGraphs, executor);
    }

    @Override
    public CompletableFuture<Void> saveGraph(final Graph graph) {
        return CompletableFuture.runAsync(() -> delegate.saveGraph(graph), executor);
    }

    @Override
    public CompletableFuture<Void> deleteGraph(final int graphId) {
        return CompletableFuture.runAsync(() -> delegate.deleteGraph(graphId), executor);
    }

    @Override
    public CompletableFuture<Void> reloadGraphs() {
        return CompletableFuture.runAsync(delegate::reloadGraphs, executor);
    }
}
