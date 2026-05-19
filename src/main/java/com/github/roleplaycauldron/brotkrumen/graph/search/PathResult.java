package com.github.roleplaycauldron.brotkrumen.graph.search;

import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;

import java.util.List;

/**
 * Structured pathfinding result.
 *
 * @param nodes    ordered node references
 * @param segments ordered traversal segments
 */
public record PathResult(List<NodeRef> nodes, List<PathSegment> segments) {

    /**
     * Creates a normalized path result.
     *
     * @param nodes    ordered node references
     * @param segments ordered traversal segments
     */
    public PathResult {
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
        segments = segments == null ? List.of() : List.copyOf(segments);
    }

    /**
     * Empty path result.
     *
     * @return empty result
     */
    public static PathResult empty() {
        return new PathResult(List.of(), List.of());
    }
}
