package com.github.roleplaycauldron.brotkrumen.visual.source;

import com.github.roleplaycauldron.brotkrumen.graph.EdgeFlag;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdge;

import java.util.List;

/**
 * Shared path edge matching rules for path-based sources.
 */
final class PathEdgeMatcher {

    private PathEdgeMatcher() {
    }

    /**
     * Checks if the edge matches an adjacent path segment in the given window.
     *
     * @param edge         candidate edge
     * @param path         path node references
     * @param start        inclusive window start
     * @param endExclusive exclusive window end
     * @return true when the edge matches the path adjacency rules
     */
    /* default */
    static boolean matchesPathWindow(final VisualEdge edge, final List<NodeRef> path, final int start,
                                     final int endExclusive) {
        for (int i = start; i + 1 < endExclusive; i++) {
            if (matchesDirected(edge, path.get(i), path.get(i + 1))) {
                return true;
            }
            if (isUndirected(edge) && matchesDirected(edge, path.get(i + 1), path.get(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isUndirected(final VisualEdge edge) {
        return edge.flags().contains(EdgeFlag.UNDIRECTED);
    }

    private static boolean matchesDirected(final VisualEdge edge, final NodeRef source, final NodeRef target) {
        return edge.source().equals(source) && edge.target().equals(target);
    }
}
