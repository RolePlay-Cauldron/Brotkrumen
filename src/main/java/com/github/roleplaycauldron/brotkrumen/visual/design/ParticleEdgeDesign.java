package com.github.roleplaycauldron.brotkrumen.visual.design;

import com.github.roleplaycauldron.spellbook.effect.EffectBuilder;
import com.github.roleplaycauldron.spellbook.effect.shape.MovingPointShape;
import com.github.roleplaycauldron.spellbook.effect.shape.Shape;
import org.bukkit.Particle;

import java.util.function.Function;

/**
 * Particle renderer design data for a visual edge.
 *
 * @param particle      particle used by the effect
 * @param effectFactory factory that builds the Spellbook effect
 */
public record ParticleEdgeDesign(Particle particle, ParticleEdgeEffectFactory effectFactory) {

    /**
     * Creates the default local particle edge design.
     *
     * @return default local particle edge design
     */
    public static ParticleEdgeDesign defaultLocal() {
        return movingPoint(Particle.FLAME, 0.15f);
    }

    /**
     * Creates the default inter-graph particle edge design.
     *
     * @return default inter-graph particle edge design
     */
    public static ParticleEdgeDesign defaultInterGraph() {
        return movingPoint(Particle.END_ROD, 0.15f);
    }

    /**
     * Creates a moving-point particle edge design.
     *
     * @param particle  particle used by the effect
     * @param thickness point spacing used by the effect
     * @return moving-point edge design
     */
    public static ParticleEdgeDesign movingPoint(final Particle particle, final float thickness) {
        return shape(particle, context -> new MovingPointShape(context.distance(), Math.max(0.05f, thickness), 8, false));
    }

    /**
     * Creates a particle edge design backed by a caller-provided Spellbook shape factory.
     *
     * @param particle     particle used by the effect
     * @param shapeFactory factory that builds the edge shape
     * @return shape-backed edge design
     */
    public static ParticleEdgeDesign shape(final Particle particle,
                                           final Function<ParticleEdgeEffectContext, Shape> shapeFactory) {
        return new ParticleEdgeDesign(particle, context -> EffectBuilder.create()
                .shape(shapeFactory.apply(context))
                .particle(particle)
                .build());
    }
}
