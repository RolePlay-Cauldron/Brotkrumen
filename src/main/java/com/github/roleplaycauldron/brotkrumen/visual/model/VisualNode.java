package com.github.roleplaycauldron.brotkrumen.visual.model;

import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;

/**
 * Node data exposed to visual renderers.
 *
 * @param id   stable visual id
 * @param ref  original node reference
 * @param node concrete node data
 * @param role semantic visual role
 */
@SuppressWarnings("PMD.ShortVariable")
public record VisualNode(VisualNodeId id, NodeRef ref, Node node, VisualNodeRole role) {

    /**
     * Creates a node with the default visual role.
     *
     * @param id   stable visual id
     * @param ref  original node reference
     * @param node concrete node data
     */
    public VisualNode(final VisualNodeId id, final NodeRef ref, final Node node) {
        this(id, ref, node, VisualNodeRole.DEFAULT);
    }
}
