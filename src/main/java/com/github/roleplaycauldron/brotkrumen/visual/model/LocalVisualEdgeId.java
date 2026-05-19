package com.github.roleplaycauldron.brotkrumen.visual.model;

import java.util.UUID;

/**
 * Stable visual identity for a graph-local edge.
 *
 * @param graphDbId graph database id
 * @param edgeId    edge id inside the graph
 */
public record LocalVisualEdgeId(int graphDbId, UUID edgeId) implements VisualEdgeId {
}
