package brotkrumen.graph.search;

import brotkrumen.graph.Edge;
import brotkrumen.graph.Graph;
import brotkrumen.graph.Node;
import brotkrumen.graph.TeleportRules;
import brotkrumen.graph.search.impl.AStarAlgorithm;
import brotkrumen.graph.search.impl.DijkstraAlgorithm;

import java.util.List;
import java.util.function.Predicate;

public class PathFinder {
    private final SearchRegistry registry;

    public PathFinder() {
        this.registry = new SearchRegistry();
        this.registry.register(new AStarAlgorithm());
        this.registry.register(new DijkstraAlgorithm());
    }

    public PathFinder(SearchRegistry registry) {
        this.registry = registry;
    }

    public List<Node> findPath(Graph graph, int start, int goal, Predicate<Edge> edgeFilter, TeleportRules rules) {
        PathAlgorithm algo = registry.select(graph, rules);
        return algo.findPath(graph, start, goal, edgeFilter, rules);
    }
}
