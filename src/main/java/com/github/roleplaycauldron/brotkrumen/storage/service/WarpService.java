package com.github.roleplaycauldron.brotkrumen.storage.service;

import com.github.roleplaycauldron.brotkrumen.graph.Warp;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * CRUD and managed read boundary for persisted warps.
 */
public interface WarpService {

    /**
     * Reads a warp by key.
     *
     * @param key warp key
     * @return persisted warp
     */
    Optional<Warp> getWarp(String key);

    /**
     * Reads all persisted managed warps before routing filters are applied.
     *
     * @return all managed warps
     */
    Set<Warp> getManagedWarps();

    /**
     * Reads warps targeting one node.
     *
     * @param targetNodeId target node id
     * @return matching warps
     */
    Set<Warp> getWarpsTargeting(UUID targetNodeId);

    /**
     * Reads warps targeting any requested node.
     *
     * @param targetNodeIds target node ids
     * @return matching warps
     */
    Set<Warp> getWarpsTargeting(Collection<UUID> targetNodeIds);

    /**
     * Creates or updates a warp by key.
     *
     * @param warp warp to persist
     */
    void saveWarp(Warp warp);

    /**
     * Removes a warp.
     *
     * @param key warp key
     * @return true when the warp existed
     */
    boolean removeWarp(String key);
}
