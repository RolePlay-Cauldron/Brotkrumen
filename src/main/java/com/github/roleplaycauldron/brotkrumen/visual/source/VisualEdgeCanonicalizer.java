package com.github.roleplaycauldron.brotkrumen.visual.source;

import com.github.roleplaycauldron.brotkrumen.graph.EdgeFlag;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdge;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeId;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeKind;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdgeRole;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Canonicalizes visual edge lists without changing graph traversal storage.
 */
final class VisualEdgeCanonicalizer {

    private VisualEdgeCanonicalizer() {
    }

    /**
     * Returns visual edges with opposite undirected traversal edges represented once.
     *
     * @param edges visual edges
     * @return canonical visual edge list
     */
    /* default */
    static List<VisualEdge> canonicalize(final List<VisualEdge> edges) {
        final List<VisualEdge> directedEdges = new ArrayList<>();
        final Map<UndirectedEdgeKey, VisualEdge> undirectedEdges = new LinkedHashMap<>();

        for (final VisualEdge edge : edges) {
            if (!edge.flags().contains(EdgeFlag.UNDIRECTED)) {
                directedEdges.add(edge);
                continue;
            }
            undirectedEdges.merge(UndirectedEdgeKey.from(edge), edge, VisualEdgeCanonicalizer::stableEdge);
        }

        final List<VisualEdge> result = new ArrayList<>(directedEdges);
        result.addAll(undirectedEdges.values());
        return List.copyOf(result);
    }

    private static VisualEdge stableEdge(final VisualEdge left, final VisualEdge right) {
        return edgeIdKey(left.id()).compareTo(edgeIdKey(right.id())) <= 0 ? left : right;
    }

    private static String edgeIdKey(final VisualEdgeId edgeId) {
        return edgeId.toString();
    }

    private static String nodeRefKey(final NodeRef ref) {
        return ref.graphDbId() + ":" + ref.nodeId();
    }

    private record UndirectedEdgeKey(NodeRef first, NodeRef second, VisualEdgeKind kind, double cost,
                                     Set<EdgeFlag> flags, VisualEdgeRole role) {

        private static UndirectedEdgeKey from(final VisualEdge edge) {
            final String sourceKey = nodeRefKey(edge.source());
            final String targetKey = nodeRefKey(edge.target());
            if (sourceKey.compareTo(targetKey) <= 0) {
                return new UndirectedEdgeKey(edge.source(), edge.target(), edge.kind(), edge.cost(), edge.flags(),
                        edge.role());
            }
            return new UndirectedEdgeKey(edge.target(), edge.source(), edge.kind(), edge.cost(), edge.flags(),
                    edge.role());
        }
    }
}
