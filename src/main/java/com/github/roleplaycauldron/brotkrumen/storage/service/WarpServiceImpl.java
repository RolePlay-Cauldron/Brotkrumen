package com.github.roleplaycauldron.brotkrumen.storage.service;

import com.github.roleplaycauldron.brotkrumen.graph.Warp;
import com.github.roleplaycauldron.brotkrumen.storage.database.Storage;
import com.github.roleplaycauldron.brotkrumen.storage.database.table.WarpTable;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Database-backed {@link WarpService}.
 */
public class WarpServiceImpl implements WarpService {

    private final Storage storage;

    private final WarpTable warpTable;

    /**
     * Creates a database-backed warp service.
     *
     * @param storage storage root
     */
    public WarpServiceImpl(final Storage storage) {
        this(storage, new WarpTable(storage.getTablePrefix() + "_warp"));
    }

    /* default */ WarpServiceImpl(final Storage storage, final WarpTable warpTable) {
        this.storage = storage;
        this.warpTable = warpTable;
    }

    @Override
    public Optional<Warp> getWarp(final String key) {
        return warpTable.findByKey(storage.getProvider(), key);
    }

    @Override
    public Set<Warp> getManagedWarps() {
        return warpTable.getAllWarps(storage.getProvider());
    }

    @Override
    public Set<Warp> getWarpsTargeting(final UUID targetNodeId) {
        return warpTable.findByTargetNodeId(storage.getProvider(), targetNodeId);
    }

    @Override
    public Set<Warp> getWarpsTargeting(final Collection<UUID> targetNodeIds) {
        if (targetNodeIds == null || targetNodeIds.isEmpty()) {
            return Set.of();
        }
        final Set<UUID> targets = Set.copyOf(targetNodeIds);
        return getManagedWarps().stream()
                .filter(warp -> targets.contains(warp.targetNodeId()))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public void saveWarp(final Warp warp) {
        warpTable.saveWarp(storage.getProvider(), warp);
    }

    @Override
    public boolean removeWarp(final String key) {
        return warpTable.deleteByKey(storage.getProvider(), key);
    }
}
