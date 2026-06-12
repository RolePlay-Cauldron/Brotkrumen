package com.github.roleplaycauldron.brotkrumen.service;

import com.github.roleplaycauldron.brotkrumen.api.service.GraphService;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.storage.repository.GraphRepository;
import com.github.roleplaycauldron.spellbook.core.scheduler.SimpleScheduler;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Executor-backed public async graph service adapter.
 */
public class AsyncGraphService implements GraphService {

    private final GraphRepository delegate;

    private final SimpleScheduler scheduler;

    /**
     * Creates an async graph service adapter.
     *
     * @param delegate  synchronous delegate
     * @param scheduler scheduler
     */
    public AsyncGraphService(final GraphRepository delegate, final SimpleScheduler scheduler) {
        this.delegate = delegate;
        this.scheduler = scheduler;
    }

    @Override
    public CompletableFuture<Optional<Graph>> graphById(final int graphId) {
        return scheduler.runTaskAsync(() -> delegate.getGraphById(graphId));
    }

    @Override
    public CompletableFuture<Optional<Graph>> graphByName(final String name) {
        return scheduler.runTaskAsync(() -> delegate.getGraphByName(name));
    }

    @Override
    public CompletableFuture<Set<Graph>> allGraphs() {
        return scheduler.runTaskAsync(delegate::getAllGraphs);
    }

    @Override
    public CompletableFuture<Void> saveGraph(final Graph graph) {
        return scheduler.runTaskAsync(() -> delegate.saveGraph(graph));
    }

    @Override
    public CompletableFuture<Void> deleteGraph(final int graphId) {
        return scheduler.runTaskAsync(() -> delegate.deleteGraph(graphId));
    }

    @Override
    public CompletableFuture<Void> reloadGraphs() {
        return scheduler.runTaskAsync(delegate::reloadGraphs);
    }
}
