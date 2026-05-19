package com.github.roleplaycauldron.brotkrumen.visual.source;

import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.graph.search.PathResult;
import com.github.roleplaycauldron.brotkrumen.graph.search.PathSegment;
import com.github.roleplaycauldron.brotkrumen.graph.search.TraversalKind;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdge;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualGraphSnapshot;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNode;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNodeRole;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Visual source that filters another source to a node-reference path.
 */
public class PathVisualGraphSource implements VisualGraphSource {

    private final VisualGraphSource delegate;

    private final List<NodeRef> path;

    private final Map<NodeRef, VisualNodeRole> pathRoles;

    /**
     * Creates a path source from a structured path result.
     *
     * @param delegate source to filter
     * @param result   structured path result
     */
    public PathVisualGraphSource(final VisualGraphSource delegate, final PathResult result) {
        this.delegate = delegate;
        final PathResult safeResult = result == null ? PathResult.empty() : result;
        this.path = safeResult.nodes();
        this.pathRoles = segmentRoles(safeResult.segments());
    }

    @Override
    public VisualGraphSnapshot snapshot() {
        final VisualGraphSnapshot snapshot = delegate.snapshot();
        final Set<NodeRef> pathRefs = new HashSet<>(path);
        final List<VisualNode> nodes = snapshot.nodes().stream()
                .filter(node -> pathRefs.contains(node.ref()))
                .map(this::pathNode)
                .toList();
        final List<VisualEdge> edges = snapshot.edges().stream()
                .filter(this::isPathEdge)
                .toList();
        return new VisualGraphSnapshot(nodes, edges, version());
    }

    @Override
    public long version() {
        return (31 * delegate.version()) + path.hashCode();
    }

    private boolean isPathEdge(final VisualEdge edge) {
        for (int i = 0; i + 1 < path.size(); i++) {
            if (edge.source().equals(path.get(i)) && edge.target().equals(path.get(i + 1))) {
                return true;
            }
        }
        return false;
    }

    private VisualNode pathNode(final VisualNode node) {
        return new VisualNode(node.id(), node.ref(), node.node(), pathRoles.getOrDefault(node.ref(), VisualNodeRole.DEFAULT));
    }

    private Map<NodeRef, VisualNodeRole> segmentRoles(final List<PathSegment> segments) {
        final Map<NodeRef, VisualNodeRole> result = new HashMap<>();
        for (final PathSegment segment : segments) {
            final VisualNodeRole role = roleFor(segment.traversalKind());
            if (role != null) {
                putRole(result, segment.source(), role);
                putRole(result, segment.target(), role);
            }
        }
        return result;
    }

    private VisualNodeRole roleFor(final TraversalKind traversalKind) {
        return switch (traversalKind) {
            case LOCAL_TELEPORT -> VisualNodeRole.LOCAL_TELEPORT;
            case INTERGRAPH_TELEPORT -> VisualNodeRole.INTERGRAPH_TELEPORT;
            case WARP -> VisualNodeRole.WARP;
            default -> null;
        };
    }

    private void putRole(final Map<NodeRef, VisualNodeRole> roles, final NodeRef ref, final VisualNodeRole role) {
        final VisualNodeRole current = roles.get(ref);
        if (current == null || precedence(role) > precedence(current)) {
            roles.put(ref, role);
        }
    }

    private int precedence(final VisualNodeRole role) {
        return switch (role) {
            case WARP -> 3;
            case INTERGRAPH_TELEPORT -> 2;
            case LOCAL_TELEPORT -> 1;
            default -> 0;
        };
    }
}
