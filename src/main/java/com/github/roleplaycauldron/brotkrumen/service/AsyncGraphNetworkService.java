package com.github.roleplaycauldron.brotkrumen.service;

import com.github.roleplaycauldron.brotkrumen.api.service.GraphNetworkService;
import com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork;
import com.github.roleplaycauldron.brotkrumen.graph.InterGraphEdge;
import com.github.roleplaycauldron.brotkrumen.storage.repository.GraphNetworkRepository;
import com.github.roleplaycauldron.spellbook.core.scheduler.SimpleScheduler;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Executor-backed public async graph network service adapter.
 */
public class AsyncGraphNetworkService implements GraphNetworkService {

    private final GraphNetworkRepository delegate;

    private final SimpleScheduler scheduler;

    /**
     * Creates an async graph network service adapter.
     *
     * @param delegate  synchronous delegate
     * @param scheduler database executor
     */
    public AsyncGraphNetworkService(final GraphNetworkRepository delegate, final SimpleScheduler scheduler) {
        this.delegate = delegate;
        this.scheduler = scheduler;
    }

    @Override
    public CompletableFuture<Collection<GraphNetwork>> graphNetworks() {
        return scheduler.runTaskAsync(delegate::loadGraphNetworks);
    }

    @Override
    public CompletableFuture<Void> saveInterGraphEdges(final GraphNetwork network) {
        return scheduler.runTaskAsync(() -> delegate.saveInterGraphEdges(network));
    }

    @Override
    public CompletableFuture<Set<InterGraphEdge>> interGraphEdges(final Collection<Integer> graphIds) {
        return scheduler.runTaskAsync(() -> delegate.loadInterGraphEdges(graphIds));
    }

    @Override
    public CompletableFuture<Void> saveInterGraphEdges(final Collection<InterGraphEdge> edges) {
        return scheduler.runTaskAsync(() -> delegate.saveInterGraphEdges(edges));
    }

    @Override
    public CompletableFuture<Integer> deleteInterGraphEdgesForGraph(final int graphId) {
        return scheduler.runTaskAsync(() -> delegate.deleteInterGraphEdgesForGraph(graphId));
    }

    @Override
    public CompletableFuture<Void> deleteInterGraphEdges(final GraphNetwork network) {
        return scheduler.runTaskAsync(() -> delegate.deleteInterGraphEdges(network));
    }

    @Override
    public CompletableFuture<Collection<GraphNetwork>> reloadGraphNetworks() {
        return scheduler.runTaskAsync(delegate::reloadGraphNetworks);
    }
}
