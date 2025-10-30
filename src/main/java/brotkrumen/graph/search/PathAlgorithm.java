package brotkrumen.graph.search;

import brotkrumen.graph.Edge;
import brotkrumen.graph.Graph;
import brotkrumen.graph.Node;
import brotkrumen.graph.TeleportRules;

import java.util.List;
import java.util.function.Predicate;

public interface PathAlgorithm {
    boolean suitable(Graph graph, TeleportRules rules);

    List<Node> findPath(Graph graph, int start, int goal, Predicate<Edge> edgeFilter, TeleportRules teleportRules);
}
