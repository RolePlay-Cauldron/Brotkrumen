package brotkrumen.graph.search;

import brotkrumen.graph.Edge;
import brotkrumen.graph.EdgeFlag;
import brotkrumen.graph.Graph;
import brotkrumen.graph.Node;
import brotkrumen.graph.TeleportRules;

import java.util.List;
import java.util.function.Predicate;

public class PathFinder {
    private final AStarSearch aStar = new AStarSearch();

    private final DijkstraSearch dijkstra = new DijkstraSearch();

    List<Integer> findPath(Graph g, int start, int goal, Predicate<Edge> edgeFilter, TeleportRules tp) {
        boolean needDijkstra = needsDijkstra(g, edgeFilter, tp);
        return (needDijkstra ? dijkstra : aStar).shortestPath(g, start, goal, edgeFilter, tp);
    }

    private boolean needsDijkstra(Graph g, Predicate<Edge> edgeFilter, TeleportRules tp) {
        if (tp != null && tp.isGlobalTeleportEnabled()) {
            return true;
        }
        if (graphHasTeleportEdges(g, edgeFilter)) {
            return true;
        }
        return !isEuclidHeuristicAdmissible(g);
    }

    private boolean graphHasTeleportEdges(Graph graph, Predicate<Edge> edgeFilter){
        for (Node node : graph.getNodes()) {
            for (Edge edge : graph.neighbors(node.getId())) {
                if (edge.hasFlag(EdgeFlag.TELEPORT) && edgeFilter.test(edge)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isEuclidHeuristicAdmissible(Graph g) {
        for (Node a : g.getNodes()) {
            for (Edge e : g.neighbors(a.getId())) {
                Node b = g.getNodeById(e.getTo());
                double dx = a.getX() - b.getX(), dy = a.getY() - b.getY(), dz = a.getZ() - b.getZ();
                double euclid = Math.sqrt(dx*dx + dy*dy + dz*dz);
                if (e.getCost() < euclid - 1e-9) {
                    return false;
                }
            }
        }
        return true;
    }
}
