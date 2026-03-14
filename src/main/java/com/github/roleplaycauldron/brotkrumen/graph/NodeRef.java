package com.github.roleplaycauldron.brotkrumen.graph;

import java.util.UUID;

/**
 * Stable node reference across multiple graphs.
 *
 * @param graphDbId the database id of the graph containing the node
 * @param nodeId    the node UUID inside that graph
 */
public record NodeRef(int graphDbId, UUID nodeId) {

}
