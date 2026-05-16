package com.github.roleplaycauldron.brotkrumen.visual.model;

import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;

/**
 * Node data exposed to visual renderers.
 *
 * @param id   stable visual id
 * @param ref  original node reference
 * @param node concrete node data
 */
@SuppressWarnings("PMD.ShortVariable")
public record VisualNode(VisualNodeId id, NodeRef ref, Node node) {
}
