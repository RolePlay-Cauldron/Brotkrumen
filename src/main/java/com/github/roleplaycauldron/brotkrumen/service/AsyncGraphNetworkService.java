package com.github.roleplaycauldron.brotkrumen.service;

import com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork;
import com.github.roleplaycauldron.brotkrumen.graph.InterGraphEdge;
import com.github.roleplaycauldron.brotkrumen.storage.repository.GraphNetworkRepository;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Executor-backed public async graph network service adapter.
 */
public class AsyncGraphNetworkService implements com.github.roleplaycauldron.brotkrumen.api.service.GraphNetworkService {

    private final GraphNetworkRepository delegate;

    private final Executor executor;

    /**
     * Creates an async graph network service adapter.
     *
     * @param delegate synchronous delegate
     * @param executor database executor
     */
    public AsyncGraphNetworkService(final GraphNetworkRepository delegate, final Executor executor) {
        this.delegate = delegate;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<Collection<GraphNetwork>> graphNetworks() {
        return CompletableFuture.supplyAsync(delegate::loadGraphNetworks, executor);
    }

    @Override
    public CompletableFuture<Void> saveInterGraphEdges(final GraphNetwork network) {
        return CompletableFuture.runAsync(() -> delegate.saveInterGraphEdges(network), executor);
    }

    @Override
    public CompletableFuture<Set<InterGraphEdge>> interGraphEdges(final Collection<Integer> graphIds) {
        return CompletableFuture.supplyAsync(() -> delegate.loadInterGraphEdges(graphIds), executor);
    }

    @Override
    public CompletableFuture<Void> saveInterGraphEdges(final Collection<InterGraphEdge> edges) {
        return CompletableFuture.runAsync(() -> delegate.saveInterGraphEdges(edges), executor);
    }

    @Override
    public CompletableFuture<Integer> deleteInterGraphEdgesForGraph(final int graphId) {
        return CompletableFuture.supplyAsync(() -> delegate.deleteInterGraphEdgesForGraph(graphId), executor);
    }

    @Override
    public CompletableFuture<Void> deleteInterGraphEdges(final GraphNetwork network) {
        return CompletableFuture.runAsync(() -> delegate.deleteInterGraphEdges(network), executor);
    }

    @Override
    public CompletableFuture<Collection<GraphNetwork>> reloadGraphNetworks() {
        return CompletableFuture.supplyAsync(delegate::reloadGraphNetworks, executor);
    }
}
