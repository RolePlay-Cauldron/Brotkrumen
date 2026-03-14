package com.github.roleplaycauldron.brotkrumen.graph;

/**
 * The flags for an edge.
 */
public enum EdgeFlag {
    /**
     * The edge is a directed edge, from node a to b.
     */
    DIRECTED,

    /**
     * The edge is an undirected edge, from node a to b and from b to a.
     */
    UNDIRECTED,

    /**
     * The edge is blocked and cannot be used.
     */
    BLOCKED,

    /**
     * The edge is a local teleportation edge between two nodes.
     */
    TELEPORT,

    /**
     * The edge is a global teleportation edge that can be used to teleport from any node.
     */
    TELEPORT_GLOBAL,

    /**
     * The edge to connect two graphs.
     */
    INTER_GRAPH
}
