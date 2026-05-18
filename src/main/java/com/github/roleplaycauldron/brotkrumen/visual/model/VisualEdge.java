package com.github.roleplaycauldron.brotkrumen.visual.model;

import com.github.roleplaycauldron.brotkrumen.graph.EdgeFlag;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;

import java.util.Set;

/**
 * Edge data exposed to visual renderers.
 *
 * @param id     stable visual id
 * @param source source node reference
 * @param target target node reference
 * @param kind   edge kind
 * @param cost   edge traversal cost
 * @param flags  edge flags
 * @param role   semantic visual role
 */
@SuppressWarnings("PMD.ShortVariable")
public record VisualEdge(VisualEdgeId id, NodeRef source, NodeRef target, VisualEdgeKind kind, double cost,
                         Set<EdgeFlag> flags, VisualEdgeRole role) {

    /**
     * Creates an edge using a default role derived from its broad edge kind.
     *
     * @param id     stable visual id
     * @param source source node reference
     * @param target target node reference
     * @param kind   edge kind
     * @param cost   edge traversal cost
     * @param flags  edge flags
     */
    public VisualEdge(final VisualEdgeId id, final NodeRef source, final NodeRef target, final VisualEdgeKind kind,
                      final double cost, final Set<EdgeFlag> flags) {
        this(id, source, target, kind, cost, flags, kind == VisualEdgeKind.INTER_GRAPH
                ? VisualEdgeRole.INTER_GRAPH
                : VisualEdgeRole.DEFAULT_LOCAL);
    }
}
