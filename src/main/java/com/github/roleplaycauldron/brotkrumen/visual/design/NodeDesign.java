package com.github.roleplaycauldron.brotkrumen.visual.design;

import org.bukkit.Material;
import org.bukkit.Particle;

/**
 * Visual design for a node.
 *
 * @param blockMaterial material for block-display rendering
 * @param particle      particle for particle rendering
 * @param scale         rendered node scale
 */
public record NodeDesign(Material blockMaterial, Particle particle, float scale) {

    /**
     * Creates the default node design.
     *
     * @return default design
     */
    public static NodeDesign defaults() {
        return new NodeDesign(Material.COAL_BLOCK, Particle.FLAME, 0.4f);
    }
}
