package com.github.roleplaycauldron.brotkrumen.visual.design;

import com.github.roleplaycauldron.spellbook.effect.EffectBuilder;
import com.github.roleplaycauldron.spellbook.effect.EffectInstance;
import com.github.roleplaycauldron.spellbook.effect.shape.CubeShape;
import com.github.roleplaycauldron.spellbook.effect.shape.Shape;
import com.github.roleplaycauldron.spellbook.effect.shape.SphereShape;
import org.bukkit.Particle;

/**
 * Particle renderer design data for a visual node.
 *
 * @param particle particle used by the effect
 * @param effect   Spellbook effect recipe used by the renderer
 */
public record ParticleNodeDesign(Particle particle, EffectInstance effect) {

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
        return shape(particle, new CubeShape(size, 12));
    }

    /**
     * Creates a sphere-shaped particle node design.
     *
     * @param particle particle used by the effect
     * @param radius   sphere radius
     * @return sphere-shaped design
     */
    public static ParticleNodeDesign sphere(final Particle particle, final float radius) {
        return shape(particle, new SphereShape(radius, 36));
    }

    /**
     * Creates a particle node design backed by a caller-provided Spellbook shape.
     *
     * @param particle particle used by the effect
     * @param shape    node shape
     * @return shape-backed node design
     */
    public static ParticleNodeDesign shape(final Particle particle, final Shape shape) {
        return new ParticleNodeDesign(particle, EffectBuilder.create()
                .shape(shape)
                .particle(particle)
                .build());
    }
}
