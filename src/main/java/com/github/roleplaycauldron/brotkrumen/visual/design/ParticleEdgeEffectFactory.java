package com.github.roleplaycauldron.brotkrumen.visual.design;

import com.github.roleplaycauldron.spellbook.effect.EffectInstance;

/**
 * Builds a Spellbook effect for a visual edge.
 */
@FunctionalInterface
public interface ParticleEdgeEffectFactory {

    /**
     * Builds an effect instance for the provided edge context.
     *
     * @param context edge effect context
     * @return effect instance
     */
    EffectInstance create(ParticleEdgeEffectContext context);
}
