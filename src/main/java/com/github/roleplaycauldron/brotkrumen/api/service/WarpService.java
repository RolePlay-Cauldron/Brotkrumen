package com.github.roleplaycauldron.brotkrumen.api.service;

import com.github.roleplaycauldron.brotkrumen.graph.Warp;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Async public service for persisted warp operations.
 */
public interface WarpService {

    /**
     * Loads a warp by key.
     *
     * @param key warp key
     * @return future warp lookup
     */
    CompletableFuture<Optional<Warp>> warp(String key);

    /**
     * Loads all managed warps.
     *
     * @return future warp set
     */
    CompletableFuture<Set<Warp>> managedWarps();

    /**
     * Loads warps targeting one node.
     *
     * @param targetNodeId target node id
     * @return future warp set
     */
    CompletableFuture<Set<Warp>> warpsTargeting(UUID targetNodeId);

    /**
     * Loads warps targeting any requested node.
     *
     * @param targetNodeIds target node ids
     * @return future warp set
     */
    CompletableFuture<Set<Warp>> warpsTargeting(Collection<UUID> targetNodeIds);

    /**
     * Saves a warp.
     *
     * @param warp warp to persist
     * @return completion future
     */
    CompletableFuture<Void> saveWarp(Warp warp);

    /**
     * Removes a warp by key.
     *
     * @param key warp key
     * @return future removal result
     */
    CompletableFuture<Boolean> removeWarp(String key);

    /**
     * Removes warps targeting any requested node.
     *
     * @param targetNodeIds target node ids
     * @return future removed row count
     */
    CompletableFuture<Integer> removeWarpsTargeting(Collection<UUID> targetNodeIds);
}
