package brotkrumen.graph.search;

import brotkrumen.graph.Edge;
import brotkrumen.graph.EdgeFlag;
import brotkrumen.graph.Graph;
import brotkrumen.graph.TeleportRules;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Predicate;

public abstract class AbstractSearch implements ShortestPathAlgorithm {

    @Override
    public List<Integer> shortestPath(Graph g, int start, int goal, Predicate<Edge> edgeFilter, TeleportRules tp) {
        Objects.requireNonNull(edgeFilter, "edgeFilter");
        if (g.getNodeById(start) == null || g.getNodeById(goal) == null) {
            throw new IllegalArgumentException("Start/Goal unknown");
        }
        if (tp == null) {
            tp = TeleportRules.disabled();
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
            if (u == goal) return reconstruct(parent, goal);
            if (!closed.add(u)) continue;

            for (Edge e : g.neighbors(u)) {
                if (!edgeFilter.test(e)) continue;
                if (e.hasFlag(EdgeFlag.BLOCKED)) continue;
                relax(u, e.getTo(), e.getCost(), gScore, fScore, parent, goal, g, open);
            }

            if (tp.isGlobalTeleportEnabled() && u != tp.getGlobalTargetNodeId()) {
                Edge virtual = new Edge(u, tp.getGlobalTargetNodeId(), tp.getGlobalTeleportCost(),
                        EnumSet.of(EdgeFlag.TELEPORT, EdgeFlag.TELEPORT_GLOBAL), Map.of("type", "global_spawn"));
                if (edgeFilter.test(virtual)) {
                    relax(u, virtual.getTo(), virtual.getCost(), gScore, fScore, parent, goal, g, open);
                }
            }
        }
        return List.of();
    }

    private void relax(int u, int v, double cost,
                       Map<Integer, Double> gScore,
                       Map<Integer, Double> fScore,
                       Map<Integer, Integer> parent,
                       int goal, Graph g, PriorityQueue<int[]> open) {
        double tentative = gScore.get(u) + cost;
        if (tentative < gScore.getOrDefault(v, Double.POSITIVE_INFINITY)) {
            parent.put(v, u);
            gScore.put(v, tentative);
            double f = tentative + heuristic(g, v, goal);
            fScore.put(v, f);
            open.add(new int[]{v});
        }
    }

    private List<Integer> reconstruct(Map<Integer, Integer> parent, int goal) {
        LinkedList<Integer> path = new LinkedList<>();
        Integer cur = goal;
        while (cur != null) { path.addFirst(cur); cur = parent.get(cur); }
        return path;
    }

    protected abstract double heuristic(Graph g, int a, int b);
}
