package com.github.roleplaycauldron.brotkrumen.visual.design;

/**
 * Strategy for selecting a design for inter-graph edges.
 */
public enum InterGraphEdgeDesignStrategy {
    /**
     * Use the explicit inter-graph edge kind design when configured.
     */
    EXPLICIT_INTER_GRAPH,

    /**
     * Use the source graph design set.
     */
    SOURCE_GRAPH,

    /**
     * Use the target graph design set.
     */
    TARGET_GRAPH,

    /**
     * Use the network design set.
     */
    NETWORK_DEFAULT
}
