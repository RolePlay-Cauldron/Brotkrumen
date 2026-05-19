package com.github.roleplaycauldron.brotkrumen.graph;

import java.util.UUID;

/**
 * Default warp implementation.
 *
 * @param key          the {@link String} name of the warp
 * @param targetNodeId the {@link UUID} id of the target node
 * @param cost         the {@link Double} cost of the warp
 * @param enabled      {@code true} if the warp is enabled, {@code false} otherwise
 * @param needPermission {@code true} if the warp requires permission, {@code false} otherwise
 */
public record Warp(String key, UUID targetNodeId, double cost, boolean enabled, boolean needPermission) {
}
