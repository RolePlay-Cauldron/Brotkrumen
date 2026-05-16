package com.github.roleplaycauldron.brotkrumen.visual.model;

import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Immutable snapshot of visual graph data.
 *
 * @param nodes   visual nodes
 * @param edges   visual edges
 * @param version source version
 */
public record VisualGraphSnapshot(Collection<VisualNode> nodes, Collection<VisualEdge> edges, long version) {

    /**
     * Creates an index of nodes by reference.
     *
     * @return node index
     */
    public Map<NodeRef, VisualNode> nodesByRef() {
        return nodes.stream().collect(Collectors.toMap(VisualNode::ref, Function.identity()));
    }
}
