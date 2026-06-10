package com.github.roleplaycauldron.brotkrumen.service;

import com.github.roleplaycauldron.brotkrumen.api.service.WarpService;
import com.github.roleplaycauldron.brotkrumen.graph.Warp;
import com.github.roleplaycauldron.brotkrumen.storage.repository.WarpRepository;
import com.github.roleplaycauldron.spellbook.core.scheduler.SimpleScheduler;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Executor-backed public async warp service adapter.
 */
public class AsyncWarpService implements WarpService {

    private final WarpRepository delegate;

    private final SimpleScheduler scheduler;

    /**
     * Creates an async warp service adapter.
     *
     * @param delegate  synchronous delegate
     * @param scheduler scheduler
     */
    public AsyncWarpService(final WarpRepository delegate, final SimpleScheduler scheduler) {
        this.delegate = delegate;
        this.scheduler = scheduler;
    }

    @Override
    public CompletableFuture<Optional<Warp>> warp(final String key) {
        return scheduler.runTaskAsync(() -> delegate.getWarp(key));
    }

    @Override
    public CompletableFuture<Set<Warp>> managedWarps() {
        return scheduler.runTaskAsync(delegate::getManagedWarps);
    }

    @Override
    public CompletableFuture<Set<Warp>> warpsTargeting(final UUID targetNodeId) {
        return scheduler.runTaskAsync(() -> delegate.getWarpsTargeting(targetNodeId));
    }

    @Override
    public CompletableFuture<Set<Warp>> warpsTargeting(final Collection<UUID> targetNodeIds) {
        return scheduler.runTaskAsync(() -> delegate.getWarpsTargeting(targetNodeIds));
    }

    @Override
    public CompletableFuture<Void> saveWarp(final Warp warp) {
        return scheduler.runTaskAsync(() -> delegate.saveWarp(warp));
    }

    @Override
    public CompletableFuture<Boolean> removeWarp(final String key) {
        return scheduler.runTaskAsync(() -> delegate.removeWarp(key));
    }

    @Override
    public CompletableFuture<Integer> removeWarpsTargeting(final Collection<UUID> targetNodeIds) {
        return scheduler.runTaskAsync(() -> delegate.removeWarpsTargeting(targetNodeIds));
    }
}
