package com.github.roleplaycauldron.brotkrumen.visual.design;

/**
 * Grouped designs for graph rendering.
 *
 * @param node           node design
 * @param localEdge      local edge design
 * @param interGraphEdge inter-graph edge design
 */
public record DesignSet(NodeDesign node, EdgeDesign localEdge, EdgeDesign interGraphEdge) {

    /**
     * Creates a default design set.
     *
     * @return default design set
     */
    public static DesignSet defaults() {
        return new DesignSet(NodeDesign.defaults(), EdgeDesign.defaultLocal(), EdgeDesign.defaultInterGraph());
    }
}
