package com.github.roleplaycauldron.brotkrumen.graph;

import java.util.Set;
import java.util.UUID;

/**
 * Represents an edge connecting two nodes across potentially different graphs.
 * This class serves as a data structure encapsulating all relevant information
 * about such an inter-graph edge, including costs and associated edge flags.
 *
 * @param edgeId  the UUID of the edge
 * @param dbId    the database ID of the edge. Default value is -1 when not set explicitly.
 * @param source  the source node reference
 * @param target  the target node reference
 * @param cost    the cost associated with traversing the edge.
 * @param flags   a {@link Set} of {@link EdgeFlag} enumerating specific properties of the edge.
 * @param enabled whether the edge is enabled for routing
 */
public record InterGraphEdge(int dbId, UUID edgeId, NodeRef source, NodeRef target, double cost, Set<EdgeFlag> flags,
                             boolean enabled) {

    /**
     * Creates a new inter-graph edge with the given parameters.
     * This constructor initializes an edge connecting nodes across different graphs,
     * setting the database ID to its default value of -1.
     *
     * @param edgeId  the UUID of the edge
     * @param source  the source node reference
     * @param target  the target node reference
     * @param cost    the cost associated with traversing the edge
     * @param flags   a Set of EdgeFlag enumerating specific properties of the edge
     * @param enabled whether the edge is enabled for routing
     */
    public InterGraphEdge(final UUID edgeId, final NodeRef source, final NodeRef target, final double cost,
                          final Set<EdgeFlag> flags, final boolean enabled) {
        this(-1, edgeId, source, target, cost, flags, enabled);
    }
}
