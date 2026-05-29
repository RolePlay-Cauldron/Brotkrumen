package com.github.roleplaycauldron.brotkrumen.command.bk;

import java.util.List;
import java.util.UUID;

/**
 * Parsed target for the resolve command.
 *
 * @param mode     target mode
 * @param graphKey graph name or id for graph targets
 * @param nodeIds  node ids for node-list targets
 */
public record ResolveTarget(Mode mode, String graphKey, List<UUID> nodeIds) {

    /**
     * Creates a graph target.
     *
     * @param graphKey graph name or id
     * @return graph target
     */
    public static ResolveTarget graph(final String graphKey) {
        return new ResolveTarget(Mode.GRAPH, graphKey, List.of());
    }

    /**
     * Creates a node-list target.
     *
     * @param nodeIds node ids
     * @return node target
     */
    public static ResolveTarget nodes(final List<UUID> nodeIds) {
        return new ResolveTarget(Mode.NODE_LIST, null, List.copyOf(nodeIds));
    }

    /**
     * Resolve target mode.
     */
    public enum Mode {
        /**
         * Graph target.
         */
        GRAPH,

        /**
         * Node-list target.
         */
        NODE_LIST
    }
}
