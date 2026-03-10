package com.github.roleplaycauldron.brotkrumen.graph.search.impl;

import com.github.roleplaycauldron.brotkrumen.graph.Edge;
import com.github.roleplaycauldron.brotkrumen.graph.EdgeFlag;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.TeleportRules;

import java.util.HashMap;
import java.util.Map;
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
        if (rules == null || rules.isWarpingEnabled() || rules.isLocalTeleportEnabled()) {
            return false;
        }
        return isEuclidHeuristicAdmissible(graph);
    }

    @Override
    protected boolean isEdgeAllowed(final Graph graph, final Edge edge, final TeleportRules rules) {
        return !edge.flags().contains(EdgeFlag.TELEPORT) && !edge.flags().contains(EdgeFlag.TELEPORT_GLOBAL);
    }

    @Override
    protected void initializeStart(final Graph graph, final UUID start, final UUID goal, final Map<UUID, Double> gScore) {
        fScores.clear();
        fScores.put(start, heuristic(graph, start, goal));
    }

    @Override
    protected void afterRelax(final Graph graph, final UUID nodeId, final UUID goal, final double tentativeG,
                              final Map<UUID, Double> gScore) {
        final double fScore = tentativeG + heuristic(graph, nodeId, goal);
        fScores.put(nodeId, fScore);
    }

    @Override
    protected double priorityScore(final UUID nodeId, final Map<UUID, Double> gScore) {
        return fScores.getOrDefault(nodeId, Double.POSITIVE_INFINITY);
    }

    @SuppressWarnings("PMD.ShortVariable")
    private boolean isEuclidHeuristicAdmissible(final Graph graph) {
        for (final Node nodeA : graph.getNodes()) {
            for (final Edge edge : graph.neighbors(nodeA.graphId())) {
                final Node nodeB = graph.getNodeById(edge.target());
                final double dx = nodeA.x() - nodeB.x();
                final double dy = nodeA.y() - nodeB.y();
                final double dz = nodeA.z() - nodeB.z();
                final double euclid = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (edge.cost() < euclid - 1e-9) {
                    return false;
                }
            }
        }
        return true;
    }
}
