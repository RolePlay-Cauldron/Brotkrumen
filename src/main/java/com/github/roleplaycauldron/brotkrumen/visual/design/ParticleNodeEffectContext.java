package com.github.roleplaycauldron.brotkrumen.visual.design;

import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNode;

/**
 * Context provided when a particle node effect is built.
 *
 * @param node visual node that will receive the effect
 */
public record ParticleNodeEffectContext(VisualNode node) {
}
