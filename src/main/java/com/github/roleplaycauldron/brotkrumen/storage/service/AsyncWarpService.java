package com.github.roleplaycauldron.brotkrumen.storage.service;

import com.github.roleplaycauldron.brotkrumen.graph.Warp;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Executor-backed public async warp service adapter.
 */
public class AsyncWarpService implements com.github.roleplaycauldron.brotkrumen.api.service.WarpService {

    private final WarpService delegate;

    private final Executor executor;

    /**
     * Creates an async warp service adapter.
     *
     * @param delegate synchronous delegate
     * @param executor database executor
     */
    public AsyncWarpService(final WarpService delegate, final Executor executor) {
        this.delegate = delegate;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<Optional<Warp>> warp(final String key) {
        return CompletableFuture.supplyAsync(() -> delegate.getWarp(key), executor);
    }

    @Override
    public CompletableFuture<Set<Warp>> managedWarps() {
        return CompletableFuture.supplyAsync(delegate::getManagedWarps, executor);
    }

    @Override
    public CompletableFuture<Set<Warp>> warpsTargeting(final UUID targetNodeId) {
        return CompletableFuture.supplyAsync(() -> delegate.getWarpsTargeting(targetNodeId), executor);
    }

    @Override
    public CompletableFuture<Set<Warp>> warpsTargeting(final Collection<UUID> targetNodeIds) {
        return CompletableFuture.supplyAsync(() -> delegate.getWarpsTargeting(targetNodeIds), executor);
    }

    @Override
    public CompletableFuture<Void> saveWarp(final Warp warp) {
        return CompletableFuture.runAsync(() -> delegate.saveWarp(warp), executor);
    }

    @Override
    public CompletableFuture<Boolean> removeWarp(final String key) {
        return CompletableFuture.supplyAsync(() -> delegate.removeWarp(key), executor);
    }

    @Override
    public CompletableFuture<Integer> removeWarpsTargeting(final Collection<UUID> targetNodeIds) {
        return CompletableFuture.supplyAsync(() -> delegate.removeWarpsTargeting(targetNodeIds), executor);
    }
}
