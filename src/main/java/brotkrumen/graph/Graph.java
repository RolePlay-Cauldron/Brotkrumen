package brotkrumen.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Graph {
    private final Map<Integer, Node> nodes;

    private final Map<Integer, List<Edge>> adj;

    public Graph() {
        this.nodes = new HashMap<>();
        this.adj = new HashMap<>();
    }

    public void addNode(Node node) {
        if (nodes.putIfAbsent(node.getId(), node) != null) {
            throw new IllegalArgumentException(String.format("Duplicate node id: %d", node.getId()));
        }
        adj.computeIfAbsent(node.getId(), k -> new ArrayList<>());
    }

    public Node getNodeById(int nodeId) {
        return nodes.get(nodeId);
    }

    public Collection<Node> getNodes() {
        return nodes.values();
    }

    public void addDirectedEdge(int from, int to, double cost) {
        addDirectedEdge(from, to, cost, EnumSet.noneOf(EdgeFlag.class), null);
    }

    public void addDirectedEdge(int from, int to, double cost, EnumSet<EdgeFlag> flags, Map<String, String> attrs) {
        requireNode(from);
        requireNode(to);
        adj.get(from).add(new Edge(from, to, cost, flags, attrs));
    }

    public void addUndirectedEdge(int nodeA, int nodeB, double cost) {
        addDirectedEdge(nodeA, nodeB, cost);
        addDirectedEdge(nodeB, nodeA, cost);
    }

    public void addTeleportEdge(int from, int to, double cost) {
        addDirectedEdge(from, to, cost, EnumSet.of(EdgeFlag.TELEPORT), Map.of("type","fixed_teleport"));
    }

    public void addTeleportEdgeBidirectional(int nodeA, int nodeB, double cost) {
        addTeleportEdge(nodeA, nodeB, cost);
        addTeleportEdge(nodeB, nodeA, cost);
    }

    public List<Edge> neighbors(int neighborId) {
        return adj.getOrDefault(neighborId, List.of());
    }

    private void requireNode(int nodeId) {
        if (!nodes.containsKey(nodeId)) {
            throw new IllegalArgumentException(String.format("Unknown node nodeId: %d", nodeId));
        }
    }
}
