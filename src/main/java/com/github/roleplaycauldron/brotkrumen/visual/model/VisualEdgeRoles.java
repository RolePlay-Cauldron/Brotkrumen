package com.github.roleplaycauldron.brotkrumen.visual.model;

import com.github.roleplaycauldron.brotkrumen.graph.EdgeFlag;

import java.util.Set;

/**
 * Shared visual edge role derivation.
 */
public final class VisualEdgeRoles {

    private VisualEdgeRoles() {
    }

    /**
     * Derives a visual role from graph edge facts.
     * <p>
     * Precedence is blocked, local teleport, inter-graph direction, undirected, directed,
     * then default local. This keeps editorial blocked styling visible even when other flags are present.
     *
     * @param kind  broad visual edge kind
     * @param flags graph edge flags
     * @return semantic visual role
     */
    public static VisualEdgeRole derive(final VisualEdgeKind kind, final Set<EdgeFlag> flags) {
        if (flags.contains(EdgeFlag.BLOCKED)) {
            return VisualEdgeRole.BLOCKED;
        }
        final boolean interGraph = kind == VisualEdgeKind.INTER_GRAPH || flags.contains(EdgeFlag.INTER_GRAPH);
        if (interGraph) {
            return interGraphRole(flags);
        }
        if (flags.contains(EdgeFlag.TELEPORT)) {
            return VisualEdgeRole.TELEPORT;
        }
        if (flags.contains(EdgeFlag.UNDIRECTED)) {
            return VisualEdgeRole.UNDIRECTED_LOCAL;
        }
        if (flags.contains(EdgeFlag.DIRECTED)) {
            return VisualEdgeRole.DIRECTED_LOCAL;
        }
        return VisualEdgeRole.DEFAULT_LOCAL;
    }

    private static VisualEdgeRole interGraphRole(final Set<EdgeFlag> flags) {
        if (flags.contains(EdgeFlag.UNDIRECTED)) {
            return VisualEdgeRole.UNDIRECTED_INTER_GRAPH;
        }
        if (flags.contains(EdgeFlag.DIRECTED)) {
            return VisualEdgeRole.DIRECTED_INTER_GRAPH;
        }
        return VisualEdgeRole.INTER_GRAPH;
    }
}
