package com.github.roleplaycauldron.brotkrumen.visual.source;

import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualEdge;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualGraphSnapshot;
import com.github.roleplaycauldron.brotkrumen.visual.model.VisualNode;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Visual source that filters another source to a node-reference path.
 */
public class PathVisualGraphSource implements VisualGraphSource {

    private final VisualGraphSource delegate;

    private final List<NodeRef> path;

    /**
     * Creates a path source.
     *
     * @param delegate source to filter
     * @param path     path node references
     */
    public PathVisualGraphSource(final VisualGraphSource delegate, final List<NodeRef> path) {
        this.delegate = delegate;
        this.path = List.copyOf(path);
    }

    @Override
    public VisualGraphSnapshot snapshot() {
        final VisualGraphSnapshot snapshot = delegate.snapshot();
        final Set<NodeRef> pathRefs = new HashSet<>(path);
        final List<VisualNode> nodes = snapshot.nodes().stream()
                .filter(node -> pathRefs.contains(node.ref()))
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
}
