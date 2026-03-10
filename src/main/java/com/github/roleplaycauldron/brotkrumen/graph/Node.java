package com.github.roleplaycauldron.brotkrumen.graph;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.UUID;

/**
 * A node in the graph created from the database.
 *
 * @param dbId    the id of the node in the database. Use -1 or another constructor instead of using this
 * @param graphId the {@link UUID} of the node
 * @param x       the x coordinate of the node
 * @param y       the y coordinate of the node
 * @param z       the z coordinate of the node
 * @param worldId the {@link UUID} of the world the node is in
 */
@SuppressWarnings("PMD.ShortVariable")
public record Node(int dbId, UUID graphId, double x, double y, double z, UUID worldId) {

    /**
     * A constructor using the x, y and z coordinates.
     *
     * @param graphId the {@link UUID} of the node within the graph
     * @param x       the x coordinate of the node
     * @param y       the y coordinate of the node
     * @param z       the z coordinate of the node
     * @param worldId the {@link UUID} of the world the node is in
     */
    public Node(final UUID graphId, final double x, final double y, final double z, final UUID worldId) {
        this(-1, graphId, x, y, z, worldId);
    }

    /**
     * An alternative constructor using the {@link Location} instead of x, y and z coordinates.
     *
     * @param dbId    the id of the node in the database. Use -1 or another constructor instead of using this
     * @param graphId the {@link UUID} of the node
     * @param loc     the {@link Location} of the node
     */
    public Node(final int dbId, final UUID graphId, final Location loc) {
        this(dbId, graphId, loc.getX(), loc.getY(), loc.getZ(), loc.getWorld().getUID());
    }

    /**
     * An alternative constructor using the {@link Location} instead of x, y and z coordinates.
     *
     * @param graphId the {@link UUID} of the node within the graph
     * @param loc     the {@link Location} of the node
     */
    public Node(final UUID graphId, final Location loc) {
        this(-1, graphId, loc);
    }

    /**
     * Creates a new {@link Node} instance using the specified {@link Location}.
     *
     * @param loc the {@link Location} that represents the position of the node
     */
    public Node(final Location loc) {
        this(null, loc);
    }

    /**
     * Converts this node position to the center location of the represented block.
     *
     * @return the centered {@link Location}
     */
    public Location toCenterLocation() {
        return new Location(Bukkit.getWorld(worldId), x, y, z).toCenterLocation();
    }

    /**
     * Converts the nodes position-values to a {@link Location}.
     *
     * @return the {@link Location}
     */
    public Location toLocation() {
        return new Location(Bukkit.getWorld(worldId), x, y, z);
    }
}
