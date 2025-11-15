package com.github.roleplaycauldron.brotkrumen.graph;

import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * Represents an edge in a graph.
 *
 * @param dbId   the database id of the edge.
 * @param edgeId the {@link UUID} of the edge.
 * @param source the {@link UUID} of the source node.
 * @param target the {@link UUID} of the target node.
 * @param cost   the cost of the edge.
 * @param flags  an {@link EnumSet} containing the {@link EdgeFlag}s of the edge.
 */
public record Edge(int dbId, UUID edgeId, UUID source, UUID target, double cost, Set<EdgeFlag> flags) {

    /**
     * Creates a new {@link Edge} with the given parameters.
     *
     * @param edgeId the {@link UUID} of the edge.
     * @param source the {@link UUID} of the source node.
     * @param target the {@link UUID} of the target node.
     * @param cost   the cost of the edge.
     * @param flags  an {@link EnumSet} containing the {@link EdgeFlag}s of the edge.
     */
    public Edge(final UUID edgeId, final UUID source, final UUID target, final double cost, final Set<EdgeFlag> flags) {
        this(-1, edgeId, source, target, cost, flags);
    }

    /**
     * Creates a new {@link Edge} with the given parameters.
     *
     * @param dbId   the database id of the edge.
     * @param edgeId the {@link UUID} of the edge.
     * @param source the {@link UUID} of the source node.
     * @param target the {@link UUID} of the target node.
     * @param cost   the cost of the edge.
     */
    public Edge(final int dbId, final UUID edgeId, final UUID source, final UUID target, final double cost) {
        this(dbId, edgeId, source, target, cost, EnumSet.noneOf(EdgeFlag.class));
    }

    /**
     * Creates a new {@link Edge} with the given parameters.
     *
     * @param edgeId the {@link UUID} of the edge.
     * @param source the {@link UUID} of the source node.
     * @param target the {@link UUID} of the target node.
     * @param cost   the cost of the edge.
     */
    public Edge(final UUID edgeId, final UUID source, final UUID target, final double cost) {
        this(-1, edgeId, source, target, cost, EnumSet.noneOf(EdgeFlag.class));
    }

    @Override
    public @NotNull String toString() {
        return String.format("%s %s->%s (%.2f) %s", edgeId.toString(), source.toString(), target.toString(), cost, flags);
    }
}
