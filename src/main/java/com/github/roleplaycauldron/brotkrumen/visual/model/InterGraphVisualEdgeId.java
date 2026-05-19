package com.github.roleplaycauldron.brotkrumen.visual.model;

import java.util.UUID;

/**
 * Stable visual identity for an inter-graph edge.
 *
 * @param edgeId inter-graph edge id
 */
public record InterGraphVisualEdgeId(UUID edgeId) implements VisualEdgeId {
}
