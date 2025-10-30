package brotkrumen.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Graph {
    private final Map<Integer, Node> nodes = new HashMap<>();

    private final Map<Integer, List<Edge>> adj = new HashMap<>();

    private final Map<Integer, Edge> edgesById = new HashMap<>();

    private int nextEdgeId = 1;

    public void addNode(Node node) {
        if (nodes.putIfAbsent(node.getId(), node) != null) {
            throw new IllegalArgumentException("Duplicate node id: " + node.getId());
        }
        adj.computeIfAbsent(node.getId(), k -> new ArrayList<>());
    }

    public Node getNodeById(int nodeId) {
        return nodes.get(nodeId);
    }

    public Collection<Node> getNodes() {
        return nodes.values();
    }

    public Edge addDirectedEdge(int from, int to, double cost) {
        return addDirectedEdge(from, to, cost, EnumSet.noneOf(EdgeFlag.class));
    }

    public Edge addDirectedEdge(int from, int to, double cost, EnumSet<EdgeFlag> flags) {
        requireNode(from);
        requireNode(to);
        int id = nextEdgeId++;
        Edge e = new Edge(id, from, to, cost, flags);
        edgesById.put(id, e);
        adj.computeIfAbsent(from, k -> new ArrayList<>()).add(e);
        return e;
    }

    public List<Edge> addUndirectedEdge(int nodeA, int nodeB, double cost) {
        Edge e1 = addDirectedEdge(nodeA, nodeB, cost);
        Edge e2 = addDirectedEdge(nodeB, nodeA, cost);
        return List.of(e1, e2);
    }

    public List<Edge> addUndirectedEdge(int nodeA, int nodeB, double cost, EnumSet<EdgeFlag> flags) {
        Edge e1 = addDirectedEdge(nodeA, nodeB, cost, flags);
        Edge e2 = addDirectedEdge(nodeB, nodeA, cost, flags);
        return List.of(e1, e2);
    }

    public List<Edge> neighbors(int nodeId) {
        return adj.getOrDefault(nodeId, List.of());
    }

    public Edge getEdgeById(int edgeId) {
        return edgesById.get(edgeId);
    }

    private void requireNode(int nodeId) {
        if (!nodes.containsKey(nodeId)) {
            throw new IllegalArgumentException("Unknown node id: " + nodeId);
        }
    }
}
