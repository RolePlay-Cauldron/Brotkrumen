package com.github.roleplaycauldron.brotkrumen.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * The rules for teleportation.
 */
public class TeleportRules {

    private final boolean enableWarps;

    private final boolean enableLocalTeleport;

    private final boolean enableInterGraphTeleport;

    private final Collection<Warp> warps;

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
        this.enableInterGraphTeleport = enableWarps;
        this.warps = warps == null ? List.of() : Collections.unmodifiableCollection(warps);
    }

    /**
     * The default constructor.
     *
     * @param enableLocalTeleport      {@code true} if local teleports are enabled, {@code false} otherwise
     * @param enableInterGraphTeleport {@code true} if intergraph teleports are enabled, {@code false} otherwise
     * @param enableWarps              {@code true} if globally callable warps are enabled, {@code false} otherwise
     * @param warps                    the allowed {@link Warp}s to use
     */
    public TeleportRules(final boolean enableLocalTeleport, final boolean enableInterGraphTeleport,
                         final boolean enableWarps, final Collection<Warp> warps) {
        this.enableWarps = enableWarps;
        this.enableLocalTeleport = enableLocalTeleport;
        this.enableInterGraphTeleport = enableInterGraphTeleport;
        this.warps = warps == null ? List.of() : Collections.unmodifiableCollection(warps);
    }

    /**
     * Creates a {@link TeleportRules} object that disables every teleportation node.
     *
     * @return the {@link TeleportRules} object
     */
    public static TeleportRules disableTeleports() {
        return new TeleportRules(false, false, false, new ArrayList<>());
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
     * Checks if intergraph teleports are enabled.
     *
     * @return {@code true} if intergraph teleports are enabled, {@code false} otherwise
     */
    public boolean isInterGraphTeleportEnabled() {
        return enableInterGraphTeleport;
    }

    /**
     * Checks if globally callable warps are enabled.
     *
     * @return {@code true} if warps are enabled, {@code false} otherwise
     */
    public boolean isWarpTeleportEnabled() {
        return enableWarps;
    }

    /**
     * Returns all registered {@link Warp}s.
     *
     * @return the {@link Warp}s
     */
    public Collection<Warp> getWarps() {
        return warps;
    }

    /**
     * Returns the {@link Warp} with the given key.
     *
     * @param key the key of the {@link Warp} to return
     * @return the {@link Warp} with the given key, or {@code null} if no {@link Warp} with the given key exists
     */
    public Optional<Warp> getWarp(final String key) {
        return warps.stream()
                .filter(warp -> warp.key().equals(key))
                .findFirst();
    }
}
