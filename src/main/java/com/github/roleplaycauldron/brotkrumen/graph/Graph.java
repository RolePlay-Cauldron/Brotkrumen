package com.github.roleplaycauldron.brotkrumen.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The class representing a graph. It includes the creation of Edges between nodes.
 */
public class Graph {
    private final Map<Integer, Node> nodes;

    private final Map<Integer, List<Edge>> adj;

    private final Map<Integer, Edge> edgesById;

    private final IdRegistry idRegistry;

    /**
     * Create a new graph.
     * The graph is initially empty.
     */
    public Graph() {
        this.nodes = new HashMap<>();
        this.adj = new HashMap<>();
        this.edgesById = new HashMap<>();
        this.idRegistry = new IdRegistry();
    }

    /**
     * Create a new graph based on the given nodes and edges. The next edge id and node id will be set to the given values.
     *
     * @param nodes      the nodes of the graph containing the graphId as a key
     * @param adjacency  the edges of the graph containing the nodeId as a key
     * @param idRegistry the id registry to use
     */
    public Graph(final Map<Integer, Node> nodes, final Map<Integer, List<Edge>> adjacency, final IdRegistry idRegistry) {
        this.nodes = new HashMap<>(nodes);
        this.adj = new HashMap<>(adjacency);
        this.edgesById = new HashMap<>();
        this.idRegistry = idRegistry;

        edgesById.putAll(adj.entrySet().stream().flatMap(entry -> entry.getValue().stream().map(edge -> Map.entry(edge.edgeId(), edge))).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    /**
     * Add a new node to the graph.
     *
     * @param node the node to add
     * @return the added {@link Node}
     */
    public Node addNode(final Node node) {
        if (idRegistry.isNodeInUse(node.graphId())) {
            throw new IllegalArgumentException("Duplicate node id: " + node.graphId());
        }

        final Node genNode;
        if (node.graphId() <= 0) {
            genNode = new Node(node.dbId(), idRegistry.getNextNodeId(), node.x(), node.y(), node.z());
        } else {
            genNode = node;
        }

        if (nodes.putIfAbsent(genNode.graphId(), genNode) != null) {
            throw new IllegalArgumentException("An node with id " + genNode.graphId() + " already exists and therefore cannot be added to the nodes again.");
        }
        adj.computeIfAbsent(genNode.graphId(), k -> new ArrayList<>());

        return genNode;
    }

    /**
     * Remove a node from the graph.
     *
     * @param node the node to remove
     */
    public void removeNode(final Node node) {
        if (node.graphId() <= 0 || !idRegistry.isNodeInUse(node.graphId())) {
            throw new IllegalArgumentException("Cannot remove node with invalid id: " + node.graphId());
        }
        nodes.remove(node.graphId());
        adj.remove(node.graphId());
        idRegistry.releaseNodeId(node.graphId());
        edgesById.values().removeIf(edge -> edge.source() == node.graphId() || edge.target() == node.graphId());
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
     * Add a new edge to the graph based on the flags.
     *
     * @param source the source node
     * @param target the target node
     * @param cost   the cost of the edge
     * @param flags  the flags of the edge
     * @return the created {@link Edge}
     */
    public List<Edge> addEdge(final int source, final int target, final double cost, final Set<EdgeFlag> flags) {
        if (flags == null || flags.isEmpty()) {
            throw new IllegalArgumentException("You need to specify at least one flag per Edge");
        }
        final EdgeFlag flag = flags.iterator().next();
        return switch (flag) {
            case BLOCKED, DIRECTED, TELEPORT_GLOBAL -> List.of(addDirectedEdge(source, target, cost, flags));
            case UNDIRECTED, TELEPORT -> addUndirectedEdge(source, target, cost, flags);
        };
    }

    /**
     * Add a new directed edge target to the graph.
     *
     * @param source the id of the source node
     * @param target the id of the target node
     * @param cost   the cost of the edge
     * @return the created {@link Edge}
     */
    public Edge addDirectedEdge(final int source, final int target, final double cost) {
        return addDirectedEdge(source, target, cost, EnumSet.noneOf(EdgeFlag.class));
    }

    /**
     * Add a new directed edge target to the graph.
     *
     * @param source the id of the source node
     * @param target the id of the target node
     * @param cost   the cost of the edge
     * @param flags  the {@link EdgeFlag}s of the edge
     * @return the created {@link Edge}
     */
    public Edge addDirectedEdge(final int source, final int target, final double cost, final Set<EdgeFlag> flags) {
        requireNode(source);
        requireNode(target);
        final int generatedId = idRegistry.getNextEdgeId();
        final Edge edge = new Edge(-1, generatedId, source, target, cost, flags);
        edgesById.put(generatedId, edge);
        adj.computeIfAbsent(source, k -> new ArrayList<>()).add(edge);

        return edge;
    }

    /**
     * Add a new undirected edge to the graph.
     *
     * @param nodeA the id of the first node
     * @param nodeB the id of the second node
     * @param cost  the cost of the edge
     * @return a {@link List} containing the two edges.
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
     * @param cost  The cost of the edge.
     * @param flags The {@link EdgeFlag}s of the edge
     * @return A {@link List} containing the two edges.
     */
    public List<Edge> addUndirectedEdge(final int nodeA, final int nodeB, final double cost, final Set<EdgeFlag> flags) {
        final Edge edgeOne = addDirectedEdge(nodeA, nodeB, cost, flags);
        final Edge edgeTwo = addDirectedEdge(nodeB, nodeA, cost, flags);

        return List.of(edgeOne, edgeTwo);
    }

    /**
     * Remove an edge from the graph.
     *
     * @param edge The edge to remove.
     */
    public void removeEdge(final Edge edge) {
        edgesById.remove(edge.edgeId());
        adj.get(edge.source()).remove(edge);
        adj.get(edge.target()).remove(edge);
        idRegistry.releaseEdgeId(edge.edgeId());
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
