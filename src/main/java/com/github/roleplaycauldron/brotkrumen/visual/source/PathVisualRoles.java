package com.github.roleplaycauldron.brotkrumen.visual.source;

import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.graph.search.PathSegment;
import com.github.roleplaycauldron.brotkrumen.graph.search.TraversalKind;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNodeRole;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared visual role derivation for path-based visual sources.
 */
final class PathVisualRoles {

    private PathVisualRoles() {
    }

    /**
     * Derives path node roles from traversal metadata.
     *
     * @param segments path segments
     * @return node roles by node reference
     */
    /* default */
    static Map<NodeRef, VisualNodeRole> fromSegments(final List<PathSegment> segments) {
        final Map<NodeRef, VisualNodeRole> result = new HashMap<>();
        for (final PathSegment segment : segments) {
            final VisualNodeRole role = roleFor(segment.traversalKind());
            if (role != null) {
                putRole(result, segment.source(), role);
                putRole(result, segment.target(), role);
            }
        }
        return Map.copyOf(result);
    }

    private static VisualNodeRole roleFor(final TraversalKind traversalKind) {
        return switch (traversalKind) {
            case LOCAL_TELEPORT -> VisualNodeRole.LOCAL_TELEPORT;
            case INTERGRAPH_TELEPORT -> VisualNodeRole.INTERGRAPH_TELEPORT;
            case WARP -> VisualNodeRole.WARP;
            default -> null;
        };
    }

    private static void putRole(final Map<NodeRef, VisualNodeRole> roles, final NodeRef ref,
                                final VisualNodeRole role) {
        final VisualNodeRole current = roles.get(ref);
        if (current == null || precedence(role) > precedence(current)) {
            roles.put(ref, role);
        }
    }

    private static int precedence(final VisualNodeRole role) {
        return switch (role) {
            case WARP -> 3;
            case INTERGRAPH_TELEPORT -> 2;
            case LOCAL_TELEPORT -> 1;
            default -> 0;
        };
    }
}
