package com.github.roleplaycauldron.brotkrumen.graph.search.impl;

import com.github.roleplaycauldron.brotkrumen.graph.Edge;
import com.github.roleplaycauldron.brotkrumen.graph.EdgeFlag;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.graph.TeleportRules;
import com.github.roleplaycauldron.brotkrumen.graph.Warp;
import com.github.roleplaycauldron.brotkrumen.graph.search.PathAlgorithm;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Implementation of the Dijkstra algorithm.
 */
public class DijkstraAlgorithm implements PathAlgorithm {

    /**
     * Default constructor.
     */
    public DijkstraAlgorithm() {
        // Empty
    }

    @Override
    public boolean suitable(final Graph graph, final TeleportRules rules) {
        return true;
    }

    @Override
    public List<Node> findPath(final Graph g, final int start, final int goal,
                               final Predicate<Edge> edgeFilter, final TeleportRules rules) {
        if (g.getNodeById(start) == null || g.getNodeById(goal) == null) return List.of();
        final Predicate<Edge> filter = (edgeFilter == null) ? e -> true : edgeFilter;

        final Map<Integer, Double> gScore = new HashMap<>();
        final Map<Integer, Integer> parent = new HashMap<>();

        final Comparator<int[]> cmp = Comparator
                .<int[]>comparingDouble(a -> gScore.getOrDefault(a[0], Double.POSITIVE_INFINITY))
                .thenComparingInt(a -> a[0]);
        final PriorityQueue<int[]> open = new PriorityQueue<>(cmp);

        gScore.put(start, 0.0);
        open.add(new int[]{start});
        final Set<Integer> closed = new HashSet<>();

        while (!open.isEmpty()) {
            final int u = open.poll()[0];
            if (u == goal) {
                return reconstructNodes(g, parent, goal);
            }
            if (!closed.add(u)) {
                continue;
            }

            for (final Edge e : g.neighbors(u)) {
                if (e.flags().contains(EdgeFlag.BLOCKED)) {
                    continue;
                }
                if (e.flags().contains(EdgeFlag.TELEPORT) && !rules.isLocalTeleportEnabled()) {
                    continue;
                }
                if (!filter.test(e)) {
                    continue;
                }
                relax(u, e, g, gScore, parent, open);
            }

            if (rules.isWarpingEnabled()) {
                for (final Warp w : rules.getWarps()) {
                    if (!w.enabled()) {
                        continue;
                    }
                    final int to = w.targetNodeId();
                    if (u == to) {
                        continue;
                    }
                    final Edge virtual = new Edge(-1, u, to, w.cost(), Set.of(EdgeFlag.TELEPORT, EdgeFlag.TELEPORT_GLOBAL));
                    if (!filter.test(virtual)) {
                        continue;
                    }
                    relax(u, virtual, g, gScore, parent, open);
                }
            }
        }
        return List.of();
    }

    private void relax(final int u, final Edge edge, final Graph graph,
                       final Map<Integer, Double> gScore,
                       final Map<Integer, Integer> parent,
                       final PriorityQueue<int[]> open) {
        final int v = edge.target();
        final double tentative = gScore.get(u) + edge.cost();
        if (tentative < gScore.getOrDefault(v, Double.POSITIVE_INFINITY)) {
            parent.put(v, u);
            gScore.put(v, tentative);
            open.add(new int[]{v});
        }
    }

    private List<Node> reconstructNodes(final Graph graph, final Map<Integer, Integer> parent, final int goal) {
        final LinkedList<Node> path = new LinkedList<>();
        Integer cur = goal;
        while (cur != null) {
            path.addFirst(graph.getNodeById(cur));
            cur = parent.get(cur);
        }
        return path;
    }
}
