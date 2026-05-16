package com.github.roleplaycauldron.brotkrumen.visual.design;

import org.bukkit.Material;
import org.bukkit.Particle;

/**
 * Visual design for an edge.
 *
 * @param blockMaterial material for block-display rendering
 * @param particle      particle for particle rendering
 * @param thickness     rendered edge thickness
 * @param nodeClearance clearance from endpoint node centers
 */
public record EdgeDesign(Material blockMaterial, Particle particle, float thickness, double nodeClearance) {

    /**
     * Creates the default local edge design.
     *
     * @return default local edge design
     */
    public static EdgeDesign defaultLocal() {
        return new EdgeDesign(Material.WHITE_STAINED_GLASS, Particle.FLAME, 0.15f, 0.8D);
    }

    /**
     * Creates the default inter-graph edge design.
     *
     * @return default inter-graph edge design
     */
    public static EdgeDesign defaultInterGraph() {
        return new EdgeDesign(Material.LIGHT_BLUE_STAINED_GLASS, Particle.END_ROD, 0.15f, 0.8D);
    }
}
