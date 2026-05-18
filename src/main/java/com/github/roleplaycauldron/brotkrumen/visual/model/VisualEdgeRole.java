package com.github.roleplaycauldron.brotkrumen.visual.model;

/**
 * Semantic role of a visual edge.
 */
public enum VisualEdgeRole {
    /**
     * Standard edge inside one graph.
     */
    DEFAULT_LOCAL,

    /**
     * Local teleport edge inside one graph.
     */
    TELEPORT,

    /**
     * Global teleport edge inside one graph.
     */
    GLOBAL_TELEPORT,

    /**
     * Edge connecting nodes from different graphs.
     */
    INTER_GRAPH
}
