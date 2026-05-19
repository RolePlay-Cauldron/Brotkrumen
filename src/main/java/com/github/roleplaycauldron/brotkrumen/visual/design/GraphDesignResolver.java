package com.github.roleplaycauldron.brotkrumen.visual.design;

import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdge;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNode;
import com.github.roleplaycauldron.brotkrumen.visual.render.EdgeRenderStrategy;

/**
 * Resolves designs for visual graph elements.
 */
public interface GraphDesignResolver {

    /**
     * Resolves a particle renderer node design.
     *
     * @param node visual node
     * @return particle node design
     */
    ParticleNodeDesign resolveParticleNode(VisualNode node);

    /**
     * Resolves a particle renderer edge design.
     *
     * @param edge visual edge
     * @return particle edge design
     */
    ParticleEdgeDesign resolveParticleEdge(VisualEdge edge);

    /**
     * Resolves a block-display renderer node design.
     *
     * @param node visual node
     * @return block-display node design
     */
    BlockNodeDesign resolveBlockNode(VisualNode node);

    /**
     * Resolves a block-display renderer edge design.
     *
     * @param edge visual edge
     * @return block-display edge design
     */
    BlockEdgeDesign resolveBlockEdge(VisualEdge edge);

    /**
     * Resolves how an edge should be rendered.
     *
     * @param edge visual edge
     * @return render strategy
     */
    default EdgeRenderStrategy resolveEdgeRenderStrategy(final VisualEdge edge) {
        return EdgeRenderStrategy.FULL_EDGE;
    }
}
