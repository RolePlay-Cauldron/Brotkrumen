package com.github.roleplaycauldron.brotkrumen.visual.design;

import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdge;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNode;

/**
 * Context provided when a particle edge effect is built.
 *
 * @param edge   visual edge that will receive the effect
 * @param source source endpoint node
 * @param target target endpoint node
 */
public record ParticleEdgeEffectContext(VisualEdge edge, VisualNode source, VisualNode target) {

    /**
     * Calculates the distance between source and target node positions.
     *
     * @return endpoint distance, clamped to a small positive value
     */
    public float distance() {
        final double deltaX = source.node().x() - target.node().x();
        final double deltaY = source.node().y() - target.node().y();
        final double deltaZ = source.node().z() - target.node().z();
        return (float) Math.max(0.1D, Math.sqrt((deltaX * deltaX) + (deltaY * deltaY) + (deltaZ * deltaZ)));
    }
}
