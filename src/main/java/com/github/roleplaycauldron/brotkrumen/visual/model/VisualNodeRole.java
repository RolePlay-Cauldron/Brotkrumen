package com.github.roleplaycauldron.brotkrumen.visual.model;

/**
 * Semantic role of a visual node.
 */
public enum VisualNodeRole {
    /**
     * Standard graph node.
     */
    DEFAULT,

    /**
     * Node connected to a local teleport edge.
     */
    LOCAL_TELEPORT,

    /**
     * Node connected to an intergraph teleport edge.
     */
    INTERGRAPH_TELEPORT,

    /**
     * Node that is a warp target or route warp entry marker.
     */
    WARP,

    /**
     * Selected final goal node of guided resolve path visualization.
     */
    GUIDED_PATH_GOAL
}
