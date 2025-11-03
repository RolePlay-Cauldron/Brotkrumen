package com.github.roleplaycauldron.brotkrumen.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The class representing a graph. It includes the creation of Edges between nodes.
 */
public class Graph {
    private final Map<Integer, Node> nodes;

    private final Map<Integer, List<Edge>> adj;

    private final Map<Integer, Edge> edgesById;

    private int nextEdgeId;

    /**
     * Create a new graph.
     * The graph is initially empty.
     */
    public Graph() {
        this.nodes = new HashMap<>();
        this.adj = new HashMap<>();
        this.edgesById = new HashMap<>();
        this.nextEdgeId = 1;
    }

    /**
     * Add a new node to the graph.
     *
     * @param node the node to add
     */
    public void addNode(final Node node) {
        if (nodes.putIfAbsent(node.id(), node) != null) {
            throw new IllegalArgumentException("Duplicate node id: " + node.id());
        }
        adj.computeIfAbsent(node.id(), k -> new ArrayList<>());
    }

    /**
     * Get a node by its id.
     *
     * @param nodeId the id of the node to get
     * @return the node with the given id
     */
    public Node getNodeById(final int nodeId) {
        return nodes.get(nodeId);
    }

    /**
     * Get all nodes in the graph.
     *
     * @return a {@link Collection} of all {@link Node}s in the graph
     */
    public Collection<Node> getNodes() {
        return nodes.values();
    }

    /**
     * Add a new directed edge target the graph.
     *
     * @param source the id of the source node
     * @param target the id of the target node
     * @param cost the cost of the edge
     * @return the created {@link Edge}
     */
    public Edge addDirectedEdge(final int source, final int target, final double cost) {
        return addDirectedEdge(source, target, cost, EnumSet.noneOf(EdgeFlag.class));
    }

    /**
     * Add a new directed edge target the graph.
     *
     * @param source the id of the source node
     * @param target the id of the target node
     * @param cost the cost of the edge
     * @param flags the {@link EdgeFlag}s of the edge
     * @return the created {@link Edge}
     */
    public Edge addDirectedEdge(final int source, final int target, final double cost, final Set<EdgeFlag> flags) {
        requireNode(source);
        requireNode(target);
        final int generatedId = nextEdgeId;
        nextEdgeId++;
        final Edge edge = new Edge(generatedId, source, target, cost, flags);
        edgesById.put(generatedId, edge);
        adj.computeIfAbsent(source, k -> new ArrayList<>()).add(edge);
        return edge;
    }

    /**
     * Add a new undirected edge to the graph.
     *
     * @param nodeA the id of the first node
     * @param nodeB the id of the second node
     * @param cost the cost of the edge
     * @return the created {@link Edge}s
     */
    public List<Edge> addUndirectedEdge(final int nodeA, final int nodeB, final double cost) {
        final Edge edgeOne = addDirectedEdge(nodeA, nodeB, cost);
        final Edge edgeTwo = addDirectedEdge(nodeB, nodeA, cost);
        return List.of(edgeOne, edgeTwo);
    }

    /**
     * Add a new undirected edge to the graph.
     *
     * @param nodeA The id of the first node.
     * @param nodeB The id of the second node.
     * @param cost The cost of the edge.
     * @param flags The {@link EdgeFlag}s of the edge.
     * @return The a {@link List} containing the created {@link Edge}s.
     */
    public List<Edge> addUndirectedEdge(final int nodeA, final int nodeB, final double cost, final Set<EdgeFlag> flags) {
        final Edge edgeOne = addDirectedEdge(nodeA, nodeB, cost, flags);
        final Edge edgeTwo = addDirectedEdge(nodeB, nodeA, cost, flags);
        return List.of(edgeOne, edgeTwo);
    }

    /**
     * Get all neighbors of a node.
     *
     * @param nodeId The id of the node.
     * @return A {@link List} containing the neighbors.
     */
    public List<Edge> neighbors(final int nodeId) {
        return adj.getOrDefault(nodeId, List.of());
    }

    /**
     * Get an edge by its id.
     *
     * @param edgeId The id of the edge.
     * @return The {@link Edge} with the given id.
     */
    public Edge getEdgeById(final int edgeId) {
        return edgesById.get(edgeId);
    }

    /**
     * Check if a node exists.
     *
     * @param nodeId The id of the node.
     * @throws IllegalArgumentException If the node does not exist.
     */
    private void requireNode(final int nodeId) {
        if (!nodes.containsKey(nodeId)) {
            throw new IllegalArgumentException("Unknown node id: " + nodeId);
        }
    }
}
