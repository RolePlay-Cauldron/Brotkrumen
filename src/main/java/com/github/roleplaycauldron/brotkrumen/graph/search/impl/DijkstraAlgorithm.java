package com.github.roleplaycauldron.brotkrumen.graph.search.impl;

import com.github.roleplaycauldron.brotkrumen.graph.Edge;
import com.github.roleplaycauldron.brotkrumen.graph.EdgeFlag;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.TeleportRules;
import com.github.roleplaycauldron.brotkrumen.graph.Warp;

import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Implementation of Dijkstra's algorithm.
 */
public class DijkstraAlgorithm extends AbstractShortestPath {

    /**
     * The default constructor.
     */
    public DijkstraAlgorithm() {
        super();
    }

    @Override
    public boolean suitable(final Graph graph, final TeleportRules rules) {
        return true;
    }

    @Override
    protected boolean isEdgeAllowed(final Graph graph, final Edge edge, final TeleportRules rules) {
        if (edge.flags().contains(EdgeFlag.BLOCKED)) {
            return false;
        }
        return !edge.flags().contains(EdgeFlag.TELEPORT) || rules.isLocalTeleportEnabled();
    }

    @Override
    protected void onExpandNode(final Graph graph, final int nodeId, final TeleportRules rules,
                                final Predicate<Edge> filter, final Map<Integer, Double> gScore,
                                final Map<Integer, Integer> parent, final Queue<int[]> open, final int goal) {
        if (!rules.isWarpingEnabled()) {
            return;
        }
        for (final Warp warp : rules.getWarps()) {
            if (!warp.enabled()) {
                continue;
            }
            final int targetNodeId = warp.targetNodeId();
            if (nodeId == targetNodeId) {
                continue;
            }
            final Edge virtualEdge = new Edge(-1, nodeId, targetNodeId, warp.cost(), Set.of(EdgeFlag.TELEPORT, EdgeFlag.TELEPORT_GLOBAL));
            if (!filter.test(virtualEdge)) {
                continue;
            }
            relax(nodeId, virtualEdge, graph, goal, gScore, parent, open);
        }
    }
}
