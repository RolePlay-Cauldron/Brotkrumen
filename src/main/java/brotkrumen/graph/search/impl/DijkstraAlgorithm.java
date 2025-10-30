package brotkrumen.graph.search.impl;

import brotkrumen.graph.Edge;
import brotkrumen.graph.EdgeFlag;
import brotkrumen.graph.Graph;
import brotkrumen.graph.Node;
import brotkrumen.graph.TeleportRules;
import brotkrumen.graph.Warp;
import brotkrumen.graph.search.PathAlgorithm;

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

public class DijkstraAlgorithm implements PathAlgorithm {

    @Override
    public boolean suitable(Graph graph, TeleportRules rules) {
        return true;
    }

    @Override
    public List<Node> findPath(Graph g, int start, int goal,
                               Predicate<Edge> edgeFilter, TeleportRules rules) {
        if (g.getNodeById(start) == null || g.getNodeById(goal) == null) return List.of();
        Predicate<Edge> filter = (edgeFilter == null) ? e -> true : edgeFilter;

        Map<Integer, Double> gScore = new HashMap<>();
        Map<Integer, Integer> parent = new HashMap<>();

        Comparator<int[]> cmp = Comparator
                .<int[]>comparingDouble(a -> gScore.getOrDefault(a[0], Double.POSITIVE_INFINITY))
                .thenComparingInt(a -> a[0]);
        PriorityQueue<int[]> open = new PriorityQueue<>(cmp);

        gScore.put(start, 0.0);
        open.add(new int[]{start});
        Set<Integer> closed = new HashSet<>();

        while (!open.isEmpty()) {
            int u = open.poll()[0];
            if (u == goal) {
                return reconstructNodes(g, parent, goal);
            }
            if (!closed.add(u)) {
                continue;
            }

            for (Edge e : g.neighbors(u)) {
                if (e.hasFlag(EdgeFlag.BLOCKED)) {
                    continue;
                }
                if (e.hasFlag(EdgeFlag.TELEPORT) && !rules.isLocalTeleportEnabled()) {
                    continue;
                }
                if (!filter.test(e)) {
                    continue;
                }
                relax(u, e, g, gScore, parent, open);
            }

            if (rules.isWarpingEnabled()) {
                for (Warp w : rules.getWarps()) {
                    if (!w.enabled()) {
                        continue;
                    }
                    int to = w.targetNodeId();
                    if (u == to) {
                        continue;
                    }
                    Edge virtual = new Edge(-1, u, to, w.cost(), EnumSet.of(EdgeFlag.TELEPORT, EdgeFlag.TELEPORT_GLOBAL));
                    if (!filter.test(virtual)) {
                        continue;
                    }
                    relax(u, virtual, g, gScore, parent, open);
                }
            }
        }
        return List.of();
    }

    private void relax(int u, Edge e, Graph g,
                       Map<Integer, Double> gScore,
                       Map<Integer, Integer> parent,
                       PriorityQueue<int[]> open) {
        int v = e.getTo();
        double tentative = gScore.get(u) + e.getCost();
        if (tentative < gScore.getOrDefault(v, Double.POSITIVE_INFINITY)) {
            parent.put(v, u);
            gScore.put(v, tentative);
            open.add(new int[]{v});
        }
    }

    private List<Node> reconstructNodes(Graph graph, Map<Integer, Integer> parent, int goal) {
        LinkedList<Node> path = new LinkedList<>();
        Integer cur = goal;
        while (cur != null) {
            path.addFirst(graph.getNodeById(cur));
            cur = parent.get(cur);
        }
        return path;
    }
}
