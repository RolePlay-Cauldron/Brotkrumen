package com.github.roleplaycauldron.brotkrumen.graph;

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

    public void addNode(final Node node) {
        if (nodes.putIfAbsent(node.getId(), node) != null) {
            throw new IllegalArgumentException("Duplicate node id: " + node.getId());
        }
        adj.computeIfAbsent(node.getId(), k -> new ArrayList<>());
    }

    public Node getNodeById(final int nodeId) {
        return nodes.get(nodeId);
    }

    public Collection<Node> getNodes() {
        return nodes.values();
    }

    public Edge addDirectedEdge(final int from, final int to, final double cost) {
        return addDirectedEdge(from, to, cost, EnumSet.noneOf(EdgeFlag.class));
    }

    public Edge addDirectedEdge(final int from, final int to, final double cost, final EnumSet<EdgeFlag> flags) {
        requireNode(from);
        requireNode(to);
        final int id = nextEdgeId++;
        final Edge e = new Edge(id, from, to, cost, flags);
        edgesById.put(id, e);
        adj.computeIfAbsent(from, k -> new ArrayList<>()).add(e);
        return e;
    }

    public List<Edge> addUndirectedEdge(final int nodeA, final int nodeB, final double cost) {
        final Edge e1 = addDirectedEdge(nodeA, nodeB, cost);
        final Edge e2 = addDirectedEdge(nodeB, nodeA, cost);
        return List.of(e1, e2);
    }

    public List<Edge> addUndirectedEdge(final int nodeA, final int nodeB, final double cost, final EnumSet<EdgeFlag> flags) {
        final Edge e1 = addDirectedEdge(nodeA, nodeB, cost, flags);
        final Edge e2 = addDirectedEdge(nodeB, nodeA, cost, flags);
        return List.of(e1, e2);
    }

    public List<Edge> neighbors(final int nodeId) {
        return adj.getOrDefault(nodeId, List.of());
    }

    public Edge getEdgeById(final int edgeId) {
        return edgesById.get(edgeId);
    }

    private void requireNode(final int nodeId) {
        if (!nodes.containsKey(nodeId)) {
            throw new IllegalArgumentException("Unknown node id: " + nodeId);
        }
    }
}
