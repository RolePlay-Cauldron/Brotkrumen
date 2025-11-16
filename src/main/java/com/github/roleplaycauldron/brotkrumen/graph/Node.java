package com.github.roleplaycauldron.brotkrumen.graph;

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
 */
@SuppressWarnings("PMD.ShortVariable")
public record Node(int dbId, UUID graphId, int x, int y, int z) {

    /**
     * A constructor using the x, y and z coordinates.
     *
     * @param graphId the {@link UUID} of the node within the graph
     * @param x       the x coordinate of the node
     * @param y       the y coordinate of the node
     * @param z       the z coordinate of the node
     */
    public Node(final UUID graphId, final int x, final int y, final int z) {
        this(-1, graphId, x, y, z);
    }

    /**
     * An alternative constructor using the {@link Location} instead of x, y and z coordinates.
     *
     * @param dbId    the id of the node in the database. Use -1 or another constructor instead of using this
     * @param graphId the {@link UUID} of the node
     * @param loc     the {@link Location} of the node
     */
    public Node(final int dbId, final UUID graphId, final Location loc) {
        this(dbId, graphId, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
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
}
