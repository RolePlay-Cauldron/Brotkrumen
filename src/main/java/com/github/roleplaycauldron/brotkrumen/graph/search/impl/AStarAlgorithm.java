package com.github.roleplaycauldron.brotkrumen.graph.search.impl;

import com.github.roleplaycauldron.brotkrumen.graph.Edge;
import com.github.roleplaycauldron.brotkrumen.graph.EdgeFlag;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.TeleportRules;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Implementation of the A* algorithm.
 */
public class AStarAlgorithm extends AbstractShortestPath {

    private final Map<UUID, Double> fScores = new HashMap<>();

    /**
     * The default constructor.
     */
    public AStarAlgorithm() {
        super();
    }

    @Override
    public boolean suitable(final Graph graph, final TeleportRules rules) {
        return rules != null && !rules.isWarpingEnabled() && !rules.isLocalTeleportEnabled();
    }

    @Override
    protected boolean isEdgeAllowed(final Graph graph, final Edge edge, final TeleportRules rules) {
        return !edge.flags().contains(EdgeFlag.TELEPORT) && !edge.flags().contains(EdgeFlag.TELEPORT_GLOBAL);
    }

    @Override
    protected void initializeStart(final Graph graph, final UUID start, final Set<UUID> goals, final Map<UUID, Double> gScore) {
        fScores.clear();
        fScores.put(start, minHeuristic(graph, start, goals));
    }

    @Override
    protected void afterRelax(final Graph graph, final UUID nodeId, final Set<UUID> goals, final double tentativeG,
                              final Map<UUID, Double> gScore) {
        final double fScore = tentativeG + minHeuristic(graph, nodeId, goals);
        fScores.put(nodeId, fScore);
    }

    private double minHeuristic(final Graph graph, final UUID nodeId, final Set<UUID> goals) {
        double min = Double.POSITIVE_INFINITY;
        for (final UUID goal : goals) {
            final double heuristic = heuristic(graph, nodeId, goal);
            if (heuristic < min) {
                min = heuristic;
            }
        }
        return min;
    }

    @Override
    protected double priorityScore(final UUID nodeId, final Map<UUID, Double> gScore) {
        return fScores.getOrDefault(nodeId, Double.POSITIVE_INFINITY);
    }
}
