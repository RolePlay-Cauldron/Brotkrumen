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
     * Directed edge inside one graph.
     */
    DIRECTED_LOCAL,

    /**
     * Undirected edge inside one graph.
     */
    UNDIRECTED_LOCAL,

    /**
     * Blocked edge that cannot currently be traversed.
     */
    BLOCKED,

    /**
     * Local teleport edge inside one graph.
     */
    TELEPORT,

    /**
     * Edge connecting nodes from different graphs.
     */
    INTER_GRAPH,

    /**
     * Directed edge connecting nodes from different graphs.
     */
    DIRECTED_INTER_GRAPH,

    /**
     * Undirected edge connecting nodes from different graphs.
     */
    UNDIRECTED_INTER_GRAPH
}
