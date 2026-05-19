package com.github.roleplaycauldron.brotkrumen.visual.design;

import com.github.roleplaycauldron.spellbook.effect.EffectBuilder;
import com.github.roleplaycauldron.spellbook.effect.EffectInstance;
import com.github.roleplaycauldron.spellbook.effect.shape.LineShape;
import com.github.roleplaycauldron.spellbook.effect.shape.MovingPointShape;
import com.github.roleplaycauldron.spellbook.effect.shape.Shape;
import org.bukkit.Particle;

/**
 * Particle renderer design data for a visual edge.
 *
 * @param particle particle used by the effect
 * @param effect   Spellbook effect recipe used by the renderer
 */
public record ParticleEdgeDesign(Particle particle, EffectInstance effect) {

    private static final float MINIMUM_MOVING_POINT_SPACING = 0.2f;

    private static final int DEFAULT_EDGE_POINTS = 20;

    /**
     * Creates the default local particle edge design.
     *
     * @return default local particle edge design
     */
    public static ParticleEdgeDesign defaultLocal() {
        return line(Particle.FLAME, DEFAULT_EDGE_POINTS);
    }

    /**
     * Creates the default inter-graph particle edge design.
     *
     * @return default inter-graph particle edge design
     */
    public static ParticleEdgeDesign defaultInterGraph() {
        return movingPoint(Particle.END_ROD, MINIMUM_MOVING_POINT_SPACING);
    }

    /**
     * Creates a moving-point particle edge design.
     *
     * @param particle particle used by the effect
     * @param spacing  point spacing used by the effect, clamped to {@code 0.2f}
     * @return moving-point edge design
     */
    public static ParticleEdgeDesign movingPoint(final Particle particle, final float spacing) {
        return shape(particle, new MovingPointShape(1.0f, Math.max(MINIMUM_MOVING_POINT_SPACING, spacing),
                DEFAULT_EDGE_POINTS, false));
    }

    /**
     * Creates a static line particle edge design.
     *
     * @param particle particle used by the effect
     * @param points   number of line points
     * @return line edge design
     */
    public static ParticleEdgeDesign line(final Particle particle, final int points) {
        return shape(particle, new LineShape(points));
    }

    /**
     * Creates a particle edge design backed by a caller-provided Spellbook shape.
     *
     * @param particle particle used by the effect
     * @param shape    edge shape
     * @return shape-backed edge design
     */
    public static ParticleEdgeDesign shape(final Particle particle, final Shape shape) {
        return new ParticleEdgeDesign(particle, EffectBuilder.create()
                .shape(shape)
                .particle(particle)
                .build());
    }
}
