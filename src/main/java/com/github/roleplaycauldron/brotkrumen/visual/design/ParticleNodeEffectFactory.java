package com.github.roleplaycauldron.brotkrumen.visual.design;

import com.github.roleplaycauldron.spellbook.effect.EffectInstance;

/**
 * Builds a Spellbook effect for a visual node.
 */
@FunctionalInterface
public interface ParticleNodeEffectFactory {

    /**
     * Builds an effect instance for the provided node context.
     *
     * @param context node effect context
     * @return effect instance
     */
    EffectInstance create(ParticleNodeEffectContext context);
}
