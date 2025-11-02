package com.github.roleplaycauldron.brotkrumen.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The rules for teleportation.
 */
public class TeleportRules {

    private final boolean enableWarps;

    private final boolean enableLocalTeleport;

    private final Map<String, Warp> warps;

    /**
     * The default constructor.
     *
     * @param enableWarps         {@code true} if warps are enabled, {@code false} otherwise
     * @param enableLocalTeleport {@code true} if local teleports are enabled, {@code false} otherwise
     * @param warps               the {@link Warp}s to use
     */
    public TeleportRules(final boolean enableWarps, final boolean enableLocalTeleport, final Collection<Warp> warps) {
        this.enableWarps = enableWarps;
        this.enableLocalTeleport = enableLocalTeleport;
        final Map<String, Warp> warpMap = new HashMap<>();
        if (warps != null) {
            for (final Warp warp : warps) warpMap.put(warp.key(), warp);
        }
        this.warps = Collections.unmodifiableMap(warpMap);
    }

    /**
     * Creates a {@link TeleportRules} object that disables every teleportation node.
     *
     * @return the {@link TeleportRules} object
     */
    public static TeleportRules disableTeleports() {
        return new TeleportRules(false, false, new ArrayList<>());
    }

    /**
     * Checks if warps are enabled. Warps are teleports that are warped and are not local teleports.
     *
     * @return {@code true} if warps are enabled, {@code false} otherwise
     */
    public boolean isWarpingEnabled() {
        return enableWarps;
    }

    /**
     * Checks if local teleports are enabled. Local teleports are teleports that are not warped but are.
     *
     * @return {@code true} if local teleports are enabled, {@code false} otherwise
     */
    public boolean isLocalTeleportEnabled() {
        return enableLocalTeleport;
    }

    /**
     * Returns all registered {@link Warp}s.
     *
     * @return the {@link Warp}s
     */
    public Collection<Warp> getWarps() {
        return warps.values();
    }

    /**
     * Returns the {@link Warp} with the given key.
     *
     * @param key the key of the {@link Warp} to return
     * @return the {@link Warp} with the given key, or {@code null} if no {@link Warp} with the given key exists
     */
    public Optional<Warp> getWarp(final String key) {
        return Optional.ofNullable(warps.get(key));
    }
}
