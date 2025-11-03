package com.github.roleplaycauldron.brotkrumen.graph;

import org.bukkit.Location;

/**
 * A node in the graph.
 *
 * @param id the id of the node.
 * @param x the x coordinate of the node.
 * @param y the y coordinate of the node.
 * @param z the z coordinate of the node.
 */
@SuppressWarnings("PMD.ShortVariable")
public record Node(int id, int x, int y, int z) {

    /**
     * An alternative constructor using the {@link Location} instead of x, y and z coordinates.
     *
     * @param id the id of the node.
     * @param loc the {@link Location} of the node.
     */
    public Node(final int id, final Location loc) {
        this(id, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
}
