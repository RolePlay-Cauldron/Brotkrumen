package com.github.roleplaycauldron.brotkrumen.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The rules for teleportation.
 */
public class TeleportRules {
    private static final String PRESET_DISABLED = "DISABLED";

    private final boolean enableWarps;

    private final boolean enableLocalTeleport;

    private final boolean enableInterGraphTeleport;

    private final Collection<Warp> warps;

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
     * Creates a {@link TeleportRules} object with the same warps as this one but with different enablement flags.
     *
     * @param local      {@code true} if local teleports are enabled
     * @param interGraph {@code true} if intergraph teleports are enabled
     * @param warps      {@code true} if globally callable warps are enabled
     * @return the new {@link TeleportRules} object
     */
    public TeleportRules withRules(final boolean local, final boolean interGraph, final boolean warps) {
        return new TeleportRules(local, interGraph, warps, this.warps);
    }

    /**
     * Parses a teleport rule string and returns a new {@link TeleportRules} object based on this one.
     *
     * @param rules the rule string to parse
     * @return the new {@link TeleportRules} object, or this object if the rule string is invalid
     */
    public TeleportRules parse(final String rules) {
        if (rules == null || rules.isBlank()) {
            return this;
        }
        final String normalized = rules.trim().toUpperCase(Locale.ROOT);
        if (PRESET_DISABLED.equals(normalized)) {
            return withRules(false, false, false);
        }
        final boolean local = normalized.contains("LOCAL");
        final boolean interGraph = normalized.contains("INTERGRAPH");
        final boolean warps = normalized.contains("WARP");
        return withRules(local, interGraph, warps);
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
