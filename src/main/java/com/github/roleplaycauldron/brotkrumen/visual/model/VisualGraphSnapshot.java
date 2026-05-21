package com.github.roleplaycauldron.brotkrumen.visual.model;

import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Immutable snapshot of visual graph data.
 *
 * @param nodes      visual nodes
 * @param edges      visual edges
 * @param version    source version
 * @param nodesByRef precomputed node index
 */
public record VisualGraphSnapshot(Collection<VisualNode> nodes, Collection<VisualEdge> edges, long version,
                                  Map<NodeRef, VisualNode> nodesByRef) {

    /**
     * Creates an immutable visual graph snapshot.
     *
     * @param nodes   visual nodes
     * @param edges   visual edges
     * @param version source version
     */
    public VisualGraphSnapshot(final Collection<VisualNode> nodes, final Collection<VisualEdge> edges,
                               final long version) {
        this(nodes, edges, version, nodeIndex(nodes));
    }

    /**
     * Creates an immutable visual graph snapshot and derives its node index from its node collection.
     *
     * @param nodes      visual nodes to expose
     * @param edges      visual edges to expose
     * @param version    source version represented by the snapshot
     * @param nodesByRef node index that must match the supplied node collection
     */
    public VisualGraphSnapshot {
        nodes = List.copyOf(nodes);
        edges = List.copyOf(edges);
        nodesByRef = Map.copyOf(nodesByRef);
        if (!nodesByRef.equals(nodeIndex(nodes))) {
            throw new IllegalArgumentException("nodesByRef must match snapshot nodes");
        }
    }

    /**
     * Builds a node-reference index for the supplied visual nodes.
     *
     * @param nodes visual nodes to index
     * @return immutable node index
     */
    private static Map<NodeRef, VisualNode> nodeIndex(final Collection<VisualNode> nodes) {
        return nodes.stream().collect(Collectors.toUnmodifiableMap(VisualNode::ref, Function.identity()));
    }
}
