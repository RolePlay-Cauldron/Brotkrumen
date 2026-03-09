package com.github.roleplaycauldron.brotkrumen.visual;

/**
 * Represents the visual mode for interacting with and managing graphical elements in the visualization system.
 * This enumeration defines the two primary modes available for graph visualization and manipulation:
 * EDIT and PATH_FINDER.
 * <p>
 * Switching between these modes affects the behavior and functionality available to the user
 * or system within the visualization framework. It influences how graphical elements are processed
 * and displayed when interacting with the graph.
 */
public enum VisualMode {
    /**
     * This mode is used for general editing of graph structures, such as adding or removing nodes and edges.
     */
    EDIT,

    /**
     * This mode focuses on pathfinding operations, allowing visualization of paths between nodes.
     */
    PATH_FINDER
}
