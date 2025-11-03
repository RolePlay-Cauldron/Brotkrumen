package com.github.roleplaycauldron.brotkrumen.graph;

import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Set;

/**
 * Represents an edge in a graph.
 *
 * @param id the id of the edge.
 * @param source the id of the source node.
 * @param target the id of the target node.
 * @param cost the cost of the edge.
 * @param flags an {@link EnumSet} containing the {@link EdgeFlag}s of the edge.
 */
@SuppressWarnings("PMD.ShortVariable")
public record Edge(int id, int source, int target, double cost, Set<EdgeFlag> flags) {

    /**
     * Creates a new {@link Edge} with the given parameters.
     *
     * @param id the id of the edge.
     * @param source the id of the source node.
     * @param target the id of the target node.
     * @param cost the cost of the edge.
     */
    public Edge(final int id, final int source, final int target, final double cost) {
        this(id, source, target, cost, EnumSet.noneOf(EdgeFlag.class));
    }

    @Override
    public @NotNull String toString() {
        return String.format("%d %d->%d (%.2f) %s", id, source, target, cost, flags);
    }
}
