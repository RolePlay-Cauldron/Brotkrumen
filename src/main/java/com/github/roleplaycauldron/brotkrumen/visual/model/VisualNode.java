package com.github.roleplaycauldron.brotkrumen.visual.model;

import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;

/**
 * Node data exposed to visual renderers.
 *
 * @param visualNodeId stable visual id
 * @param ref          original node reference
 * @param node         concrete node data
 * @param role         semantic visual role
 */
public record VisualNode(VisualNodeId visualNodeId, NodeRef ref, Node node, VisualNodeRole role) {

    /**
     * Creates a node with the default visual role.
     *
     * @param visualNodeId stable visual visualNodeId
     * @param ref          original node reference
     * @param node         concrete node data
     */
    public VisualNode(final VisualNodeId visualNodeId, final NodeRef ref, final Node node) {
        this(visualNodeId, ref, node, VisualNodeRole.DEFAULT);
    }
}
