package com.github.roleplaycauldron.brotkrumen.graph.search;

/**
 * How a path segment was traversed.
 */
public enum TraversalKind {
    /**
     * Stored non-teleport edge inside one graph.
     */
    NORMAL,

    /**
     * Stored non-teleport edge between graphs.
     */
    INTERGRAPH_NORMAL,

    /**
     * Stored local teleport edge.
     */
    LOCAL_TELEPORT,

    /**
     * Stored intergraph teleport edge.
     */
    INTERGRAPH_TELEPORT,

    /**
     * Globally callable warp traversal.
     */
    WARP
}
