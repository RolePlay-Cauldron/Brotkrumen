package com.github.roleplaycauldron.brotkrumen.visual.design;

import org.bukkit.Material;

/**
 * Block-display design data for a visual edge.
 *
 * @param blockMaterial material used by the block display
 * @param thickness     rendered edge thickness
 * @param nodeClearance clearance from endpoint node centers
 */
public record BlockEdgeDesign(Material blockMaterial, float thickness, double nodeClearance) {

    /**
     * Creates the default local block-display edge design.
     *
     * @return default local block-display edge design
     */
    public static BlockEdgeDesign defaultLocal() {
        return new BlockEdgeDesign(Material.WHITE_STAINED_GLASS, 0.15f, 0.8D);
    }

    /**
     * Creates the default inter-graph block-display edge design.
     *
     * @return default inter-graph block-display edge design
     */
    public static BlockEdgeDesign defaultInterGraph() {
        return new BlockEdgeDesign(Material.LIGHT_BLUE_STAINED_GLASS, 0.15f, 0.8D);
    }
}
