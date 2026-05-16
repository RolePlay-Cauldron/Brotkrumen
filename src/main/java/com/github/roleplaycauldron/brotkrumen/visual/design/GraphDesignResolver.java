package com.github.roleplaycauldron.brotkrumen.visual.design;

import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdge;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNode;

/**
 * Resolves designs for visual graph elements.
 */
public interface GraphDesignResolver {

    /**
     * Resolves a node design.
     *
     * @param node visual node
     * @return design
     */
    NodeDesign resolveNode(VisualNode node);

    /**
     * Resolves an edge design.
     *
     * @param edge visual edge
     * @return design
     */
    EdgeDesign resolveEdge(VisualEdge edge);
}
