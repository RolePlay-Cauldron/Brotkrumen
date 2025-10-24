package brotkrumen.graph.search;

import brotkrumen.graph.Edge;
import brotkrumen.graph.Graph;
import brotkrumen.graph.TeleportRules;

import java.util.List;
import java.util.function.Predicate;

public interface ShortestPathAlgorithm {
    List<Integer> shortestPath(Graph g, int start, int goal, Predicate<Edge> edgeFilter, TeleportRules tp);
}
