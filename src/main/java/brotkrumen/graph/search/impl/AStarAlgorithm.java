package brotkrumen.graph.search.impl;

import brotkrumen.graph.Edge;
import brotkrumen.graph.EdgeFlag;
import brotkrumen.graph.Graph;
import brotkrumen.graph.Node;
import brotkrumen.graph.TeleportRules;
import brotkrumen.graph.search.PathAlgorithm;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Predicate;

public class AStarAlgorithm implements PathAlgorithm {

    @Override
    public boolean suitable(Graph graph, TeleportRules rules) {
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
    public List<Node> findPath(Graph g, int start, int goal, Predicate<Edge> edgeFilter, TeleportRules rules) {
        if (g.getNodeById(start) == null || g.getNodeById(goal) == null) {
            return List.of();
        }

        Map<Integer, Double> gScore = new HashMap<>();
        Map<Integer, Double> fScore = new HashMap<>();
        Map<Integer, Integer> parent = new HashMap<>();

        Comparator<int[]> cmp = Comparator
                .<int[]>comparingDouble(a -> fScore.getOrDefault(a[0], Double.POSITIVE_INFINITY))
                .thenComparingInt(a -> a[0]);
        PriorityQueue<int[]> open = new PriorityQueue<>(cmp);

        gScore.put(start, 0.0);
        fScore.put(start, heuristic(g, start, goal));
        open.add(new int[]{start});
        Set<Integer> closed = new HashSet<>();

        while (!open.isEmpty()) {
            int u = open.poll()[0];
            if (u == goal) return reconstructNodes(g, parent, goal);
            if (!closed.add(u)) continue;

            for (Edge e : g.neighbors(u)) {
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

    private void relax(int u, Edge e, Graph g, int goal,
                       Map<Integer, Double> gScore, Map<Integer, Double> fScore,
                       Map<Integer, Integer> parent, PriorityQueue<int[]> open) {
        int v = e.getTo();
        double tentative = gScore.get(u) + e.getCost();
        if (tentative < gScore.getOrDefault(v, Double.POSITIVE_INFINITY)) {
            parent.put(v, u);
            gScore.put(v, tentative);
            double f = tentative + heuristic(g, v, goal);
            fScore.put(v, f);
            open.add(new int[]{v});
        }
    }

    private double heuristic(Graph g, int a, int b) {
        Node na = g.getNodeById(a), nb = g.getNodeById(b);
        double dx = na.getX() - nb.getX();
        double dy = na.getY() - nb.getY();
        double dz = na.getZ() - nb.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private List<Node> reconstructNodes(Graph g, Map<Integer, Integer> parent, int goal) {
        LinkedList<Node> path = new LinkedList<>();
        Integer cur = goal;
        while (cur != null) {
            path.addFirst(g.getNodeById(cur));
            cur = parent.get(cur);
        }
        return path;
    }

    private boolean isEuclidHeuristicAdmissible(Graph graph) {
        for (Node a : graph.getNodes()) {
            for (Edge e : graph.neighbors(a.getId())) {
                Node b = graph.getNodeById(e.getTo());
                double dx = a.getX() - b.getX();
                double dy = a.getY() - b.getY();
                double dz = a.getZ() - b.getZ();
                double euclid = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (e.getCost() < euclid - 1e-9) {
                    return false;
                }
            }
        }
        return true;
    }
}
