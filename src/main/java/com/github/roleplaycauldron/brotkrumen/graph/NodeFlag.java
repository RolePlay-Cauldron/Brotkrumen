package com.github.roleplaycauldron.brotkrumen.graph;

/**
 * Persistent capabilities assigned to graph nodes.
 */
public enum NodeFlag {
    /**
     * Node is a local teleport endpoint.
     */
    LOCAL_TELEPORT,

    /**
     * Node is an intergraph teleport endpoint.
     */
    INTERGRAPH_TELEPORT,

    /**
     * Node is a warp target.
     */
    WARP
}
