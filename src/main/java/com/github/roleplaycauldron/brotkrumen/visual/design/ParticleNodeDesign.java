package com.github.roleplaycauldron.brotkrumen.visual.design;

import com.github.roleplaycauldron.spellbook.effect.EffectBuilder;
import com.github.roleplaycauldron.spellbook.effect.shape.CubeShape;
import com.github.roleplaycauldron.spellbook.effect.shape.Shape;
import com.github.roleplaycauldron.spellbook.effect.shape.SphereShape;
import org.bukkit.Particle;

import java.util.function.Function;

/**
 * Particle renderer design data for a visual node.
 *
 * @param particle      particle used by the effect
 * @param effectFactory factory that builds the Spellbook effect
 */
public record ParticleNodeDesign(Particle particle, ParticleNodeEffectFactory effectFactory) {

    /**
     * Creates the default particle node design.
     *
     * @return default particle node design
     */
    public static ParticleNodeDesign defaults() {
        return cube(Particle.FLAME, 0.4f);
    }

    /**
     * Creates a cube-shaped particle node design.
     *
     * @param particle particle used by the effect
     * @param size     cube size
     * @return cube-shaped design
     */
    public static ParticleNodeDesign cube(final Particle particle, final float size) {
        return shape(particle, context -> new CubeShape(size, 12));
    }

    /**
     * Creates a sphere-shaped particle node design.
     *
     * @param particle particle used by the effect
     * @param radius   sphere radius
     * @return sphere-shaped design
     */
    public static ParticleNodeDesign sphere(final Particle particle, final float radius) {
        return shape(particle, context -> new SphereShape(radius, 36));
    }

    /**
     * Creates a particle node design backed by a caller-provided Spellbook shape factory.
     *
     * @param particle     particle used by the effect
     * @param shapeFactory factory that builds the node shape
     * @return shape-backed node design
     */
    public static ParticleNodeDesign shape(final Particle particle,
                                           final Function<ParticleNodeEffectContext, Shape> shapeFactory) {
        return new ParticleNodeDesign(particle, context -> EffectBuilder.create()
                .shape(shapeFactory.apply(context))
                .particle(particle)
                .build());
    }
}
