package com.github.roleplaycauldron.brotkrumen.graph.search;

import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;

import java.util.UUID;

/**
 * One traversed step in a path result.
 *
 * @param source        source node reference
 * @param target        target node reference
 * @param traversalKind traversal kind
 * @param edgeId        backing edge id, or {@code null} for virtual traversals
 * @param warpKey       backing warp key, or {@code null} for non-warp traversals
 */
public record PathSegment(NodeRef source, NodeRef target, TraversalKind traversalKind, UUID edgeId, String warpKey) {

}
