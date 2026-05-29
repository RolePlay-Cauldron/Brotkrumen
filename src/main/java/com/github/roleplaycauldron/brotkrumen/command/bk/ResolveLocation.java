package com.github.roleplaycauldron.brotkrumen.command.bk;

import org.bukkit.Location;

import java.util.UUID;

/**
 * Thread-safe snapshot of a player location.
 *
 * @param worldId world id
 * @param x       x coordinate
 * @param y       y coordinate
 * @param z       z coordinate
 */
@SuppressWarnings("PMD.ShortVariable")
public record ResolveLocation(UUID worldId, double x, double y, double z) {

    /**
     * Creates a snapshot from a Bukkit location.
     *
     * @param location location
     * @return location snapshot
     */
    public static ResolveLocation from(final Location location) {
        return new ResolveLocation(location.getWorld().getUID(), location.getX(), location.getY(), location.getZ());
    }
}
