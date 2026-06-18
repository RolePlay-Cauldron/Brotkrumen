package com.github.roleplaycauldron.brotkrumen.visual.design;

import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeRole;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNodeRole;

import java.util.Map;

/**
 * Role-keyed block-display designs for one graph or network.
 *
 * @param nodeDesigns block-display node designs by role
 * @param edgeDesigns block-display edge designs by role
 */
public record BlockDisplayDesignSet(Map<VisualNodeRole, BlockNodeDesign> nodeDesigns,
                                    Map<VisualEdgeRole, BlockEdgeDesign> edgeDesigns) {

    /**
     * Resolves a block-display node design by role.
     *
     * @param role visual node role
     * @return matching design or default node design
     */
    public BlockNodeDesign nodeDesign(final VisualNodeRole role) {
        return nodeDesigns.getOrDefault(role, nodeDesigns.get(VisualNodeRole.DEFAULT));
    }

    /**
     * Resolves a block-display edge design by role.
     *
     * @param role visual edge role
     * @return matching design or default local edge design
     */
    public BlockEdgeDesign edgeDesign(final VisualEdgeRole role) {
        return edgeDesigns.getOrDefault(role, edgeDesigns.get(VisualEdgeRole.DEFAULT_LOCAL));
    }
}
