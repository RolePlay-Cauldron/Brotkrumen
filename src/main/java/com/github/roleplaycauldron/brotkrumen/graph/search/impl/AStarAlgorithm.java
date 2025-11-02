package com.github.roleplaycauldron.brotkrumen.graph.search.impl;

import com.github.roleplaycauldron.brotkrumen.graph.Edge;
import com.github.roleplaycauldron.brotkrumen.graph.EdgeFlag;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.TeleportRules;
import com.github.roleplaycauldron.brotkrumen.graph.search.PathAlgorithm;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Implementation of the A* algorithm.
 */
public class AStarAlgorithm implements PathAlgorithm {

    /**
     * Default constructor.
     */
    public AStarAlgorithm() {
        // Empty
    }

    @Override
    public boolean suitable(final Graph graph, final TeleportRules rules) {
        if (rules == null) {
            return false;
        }
        if (rules.isWarpingEnabled()) {
            return false;
        }
        if (rules.isLocalTeleportEnabled()) {
            return false;
        }
        return isEuclidHeuristicAdmissible(graph);
    }

    @Override
    public List<Node> findPath(final Graph g, final int start, final int goal, final Predicate<Edge> edgeFilter, final TeleportRules rules) {
        if (g.getNodeById(start) == null || g.getNodeById(goal) == null) {
            return List.of();
        }

        final Map<Integer, Double> gScore = new HashMap<>();
        final Map<Integer, Double> fScore = new HashMap<>();
        final Map<Integer, Integer> parent = new HashMap<>();

        final Comparator<int[]> cmp = Comparator
                .<int[]>comparingDouble(a -> fScore.getOrDefault(a[0], Double.POSITIVE_INFINITY))
                .thenComparingInt(a -> a[0]);
        final PriorityQueue<int[]> open = new PriorityQueue<>(cmp);

        gScore.put(start, 0.0);
        fScore.put(start, heuristic(g, start, goal));
        open.add(new int[]{start});
        final Set<Integer> closed = new HashSet<>();

        while (!open.isEmpty()) {
            final int u = open.poll()[0];
            if (u == goal) return reconstructNodes(g, parent, goal);
            if (!closed.add(u)) continue;

            for (final Edge e : g.neighbors(u)) {
                if (e.hasFlag(EdgeFlag.TELEPORT) || e.hasFlag(EdgeFlag.TELEPORT_GLOBAL)) {
                    continue;
                }
                if (edgeFilter != null && !edgeFilter.test(e)) {
                    continue;
                }
                relax(u, e, g, goal, gScore, fScore, parent, open);
            }
        }
        return List.of();
    }

    private void relax(final int u, final Edge e, final Graph g, final int goal,
                       final Map<Integer, Double> gScore, final Map<Integer, Double> fScore,
                       final Map<Integer, Integer> parent, final PriorityQueue<int[]> open) {
        final int v = e.getTo();
        final double tentative = gScore.get(u) + e.getCost();
        if (tentative < gScore.getOrDefault(v, Double.POSITIVE_INFINITY)) {
            parent.put(v, u);
            gScore.put(v, tentative);
            final double f = tentative + heuristic(g, v, goal);
            fScore.put(v, f);
            open.add(new int[]{v});
        }
    }

    private double heuristic(final Graph g, final int a, final int b) {
        final Node na = g.getNodeById(a);
        final Node nb = g.getNodeById(b);
        final double dx = na.getX() - nb.getX();
        final double dy = na.getY() - nb.getY();
        final double dz = na.getZ() - nb.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private List<Node> reconstructNodes(final Graph g, final Map<Integer, Integer> parent, final int goal) {
        final LinkedList<Node> path = new LinkedList<>();
        Integer cur = goal;
        while (cur != null) {
            path.addFirst(g.getNodeById(cur));
            cur = parent.get(cur);
        }
        return path;
    }

    private boolean isEuclidHeuristicAdmissible(final Graph graph) {
        for (final Node a : graph.getNodes()) {
            for (final Edge e : graph.neighbors(a.getId())) {
                final Node b = graph.getNodeById(e.getTo());
                final double dx = a.getX() - b.getX();
                final double dy = a.getY() - b.getY();
                final double dz = a.getZ() - b.getZ();
                final double euclid = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (e.getCost() < euclid - 1e-9) {
                    return false;
                }
            }
        }
        return true;
    }
}
