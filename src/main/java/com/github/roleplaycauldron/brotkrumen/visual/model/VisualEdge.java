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
 */
@SuppressWarnings("PMD.ShortVariable")
public record VisualEdge(VisualEdgeId id, NodeRef source, NodeRef target, VisualEdgeKind kind, double cost,
                         Set<EdgeFlag> flags) {
}
