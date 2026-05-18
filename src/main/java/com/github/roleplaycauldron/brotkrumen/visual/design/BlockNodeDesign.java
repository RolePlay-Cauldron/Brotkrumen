package com.github.roleplaycauldron.brotkrumen.visual.design;

import org.bukkit.Material;

/**
 * Block-display design data for a visual node.
 *
 * @param blockMaterial material used by the block display
 * @param scale         rendered node scale
 */
public record BlockNodeDesign(Material blockMaterial, float scale) {

    /**
     * Creates the default block-display node design.
     *
     * @return default block-display node design
     */
    public static BlockNodeDesign defaults() {
        return new BlockNodeDesign(Material.COAL_BLOCK, 0.4f);
    }
}
