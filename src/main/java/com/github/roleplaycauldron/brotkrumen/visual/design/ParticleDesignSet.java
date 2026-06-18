package com.github.roleplaycauldron.brotkrumen.visual.design;

import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeRole;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNodeRole;

import java.util.Map;

/**
 * Role-keyed particle designs for one graph or network.
 *
 * @param nodeDesigns particle node designs by role
 * @param edgeDesigns particle edge designs by role
 */
public record ParticleDesignSet(Map<VisualNodeRole, ParticleNodeDesign> nodeDesigns,
                                Map<VisualEdgeRole, ParticleEdgeDesign> edgeDesigns) {

    /**
     * Resolves a particle node design by role.
     *
     * @param role visual node role
     * @return matching design or default node design
     */
    public ParticleNodeDesign nodeDesign(final VisualNodeRole role) {
        return nodeDesigns.getOrDefault(role, nodeDesigns.get(VisualNodeRole.DEFAULT));
    }

    /**
     * Resolves a particle edge design by role.
     *
     * @param role visual edge role
     * @return matching design or default local edge design
     */
    public ParticleEdgeDesign edgeDesign(final VisualEdgeRole role) {
        return edgeDesigns.getOrDefault(role, edgeDesigns.get(VisualEdgeRole.DEFAULT_LOCAL));
    }
}
