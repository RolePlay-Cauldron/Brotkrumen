package com.github.roleplaycauldron.brotkrumen.command.bk.resolve;

import java.util.List;
import java.util.UUID;

/**
 * Parsed target for the resolve command.
 *
 * @param mode          target mode
 * @param graphKey      graph name or id for graph targets
 * @param nodeIds       node ids for node-list targets
 * @param teleportRules teleport rules
 */
public record ResolveTarget(Mode mode, String graphKey, List<UUID> nodeIds, String teleportRules) {

    /**
     * Creates a graph target.
     *
     * @param graphKey graph name or id
     * @return graph target
     */
    /* default */
    static ResolveTarget graph(final String graphKey) {
        return new ResolveTarget(Mode.GRAPH, graphKey, List.of(), null);
    }

    /**
     * Creates a graph target with teleport rules.
     *
     * @param graphKey      graph name or id
     * @param teleportRules teleport rules
     * @return graph target
     */
    /* default */
    static ResolveTarget graph(final String graphKey, final String teleportRules) {
        return new ResolveTarget(Mode.GRAPH, graphKey, List.of(), teleportRules);
    }

    /**
     * Creates a node-list target.
     *
     * @param nodeIds node ids
     * @return node target
     */
    /* default */
    static ResolveTarget nodes(final List<UUID> nodeIds) {
        return new ResolveTarget(Mode.NODE_LIST, null, List.copyOf(nodeIds), null);
    }

    /**
     * Creates a node-list target with teleport rules.
     *
     * @param nodeIds       node ids
     * @param teleportRules teleport rules
     * @return node target
     */
    /* default */
    static ResolveTarget nodes(final List<UUID> nodeIds, final String teleportRules) {
        return new ResolveTarget(Mode.NODE_LIST, null, List.copyOf(nodeIds), teleportRules);
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
