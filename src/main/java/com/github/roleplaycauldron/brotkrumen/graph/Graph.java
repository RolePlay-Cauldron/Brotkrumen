package com.github.roleplaycauldron.brotkrumen.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The class representing a graph. It includes the creation of Edges between nodes.
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.GodClass"})
public class Graph {
    private final int graphId;

    private final Map<UUID, Node> nodes;

    private final Map<UUID, List<Edge>> adj;

    private final Map<UUID, Edge> edgesById;

    private String name;

    private long modCount;

    /**
     * Create a new graph.
     * The graph is initially empty.
     *
     * @param graphId the database id of the graph. If you create a new one not saved yet, use -1 for the id.
     * @param name    the name of the graph
     */
    public Graph(final int graphId, final String name) {
        this.graphId = graphId;
        this.name = name;
        this.nodes = new HashMap<>();
        this.adj = new HashMap<>();
        this.edgesById = new HashMap<>();
        this.modCount = 0L;
    }

    /**
     * Create a new graph.
     * The graph is initially empty.
     *
     * @param name the name of the graph
     */
    public Graph(final String name) {
        this(-1, name);
    }

    /**
     * Create a new graph based on the given nodes and edges. The next edge id and node id will be set to the given values.
     *
     * @param graphId   the database id of the graph. If you create a new one not saved yet, use -1 for the id.
     * @param name      the name of the graph
     * @param nodes     the nodes of the graph containing the graphId as a key
     * @param adjacency the edges of the graph containing the nodeId as a key
     */
    public Graph(final int graphId, final String name, final Map<UUID, Node> nodes, final Map<UUID, List<Edge>> adjacency) {
        this.graphId = graphId;
        this.name = name;
        this.nodes = new HashMap<>(nodes);
        this.adj = adjacency.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> new ArrayList<>(entry.getValue())));
        this.edgesById = new HashMap<>();
        this.modCount = 0L;

        edgesById.putAll(adj.entrySet().stream().flatMap(entry -> entry.getValue().stream().map(edge -> Map.entry(edge.edgeId(), edge))).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    /**
     * Add a new node to the graph.
     *
     * @param node the node to add
     * @return the added {@link Node}
     */
    public Node addNode(final Node node) {
        if (nodes.containsKey(node.graphId())) {
            throw new IllegalArgumentException("Duplicate node id: " + node.graphId());
        }

        final Node genNode;
        if (node.graphId() == null) {
            genNode = new Node(node.dbId(), UUID.randomUUID(), node.x(), node.y(), node.z(), node.worldId(),
                    node.flags());
        } else {
            genNode = node;
        }

        if (nodes.putIfAbsent(genNode.graphId(), genNode) != null) {
            throw new IllegalArgumentException("An node with id " + genNode.graphId().toString() + " already exists and therefore cannot be added to the nodes again.");
        }
        adj.computeIfAbsent(genNode.graphId(), k -> new ArrayList<>());
        modCount++;

        return genNode;
    }

    /**
     * Remove a node from the graph including all edges connected to it.
     *
     * @param node the node to remove
     */
    public void removeNode(final Node node) {
        if (node.graphId() == null || !nodes.containsKey(node.graphId())) {
            throw new IllegalArgumentException("Cannot remove node with invalid graphId: " + node.graphId());
        }

        final UUID graphId = node.graphId();
        final Set<UUID> toRemove = edgesById.values().stream()
                .filter(edge -> edge.source().equals(graphId) || edge.target().equals(graphId))
                .map(Edge::edgeId)
                .collect(Collectors.toSet());

        toRemove.forEach(edgesById::remove);

        for (final List<Edge> edges : adj.values()) {
            edges.removeIf(e -> toRemove.contains(e.edgeId()));
        }

        adj.remove(graphId);
        nodes.remove(graphId);
        modCount++;
    }

    /**
     * Get a node by its id.
     *
     * @param nodeId the id of the node to get
     * @return the node with the given id
     */
    public Node getNodeById(final UUID nodeId) {
        return nodes.get(nodeId);
    }

    /**
     * Replaces a node while preserving its graph-local identity and edges.
     *
     * @param node node with updated metadata
     * @return updated node
     */
    public Node updateNode(final Node node) {
        if (node == null || node.graphId() == null || !nodes.containsKey(node.graphId())) {
            throw new IllegalArgumentException("Cannot update unknown node");
        }
        nodes.put(node.graphId(), node);
        modCount++;
        return node;
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
     * Get all edges in the graph.
     *
     * @return a {@link Collection} of all {@link Edge}s in the graph
     */
    public Collection<Edge> getEdges() {
        return edgesById.values();
    }

    /**
     * Returns all local edge records between two graph nodes in either direction.
     *
     * @param nodeA first node id
     * @param nodeB second node id
     * @return immutable list of matching edge records
     */
    public List<Edge> getEdgesBetween(final UUID nodeA, final UUID nodeB) {
        requireNode(nodeA);
        requireNode(nodeB);
        return edgesById.values().stream()
                .filter(edge -> isBetween(edge, nodeA, nodeB))
                .toList();
    }

    /**
     * Removes all local edge records between two graph nodes in either direction.
     *
     * @param nodeA first node id
     * @param nodeB second node id
     * @return removed edge records
     */
    public List<Edge> removeEdgesBetween(final UUID nodeA, final UUID nodeB) {
        final List<Edge> edges = getEdgesBetween(nodeA, nodeB);
        edges.forEach(this::removeEdge);
        return edges;
    }

    /**
     * Replaces all local edge records between two nodes with one directed relationship.
     *
     * @param source source node id
     * @param target target node id
     * @param cost   edge cost
     * @param flags  flags to preserve, excluding normalized relationship type flags
     * @return created directed edge
     */
    public Edge replaceDirectedRelationship(final UUID source, final UUID target, final double cost,
                                            final Set<EdgeFlag> flags) {
        removeEdgesBetween(source, target);
        return addDirectedEdge(source, target, cost, relationshipFlags(flags, EdgeFlag.DIRECTED));
    }

    /**
     * Replaces all local edge records between two nodes with an undirected relationship.
     *
     * @param nodeA first node id
     * @param nodeB second node id
     * @param cost  edge cost
     * @param flags flags to preserve, excluding normalized relationship type flags
     * @return created reciprocal undirected edges
     */
    public List<Edge> replaceUndirectedRelationship(final UUID nodeA, final UUID nodeB, final double cost,
                                                    final Set<EdgeFlag> flags) {
        removeEdgesBetween(nodeA, nodeB);
        return addUndirectedEdge(nodeA, nodeB, cost, relationshipFlags(flags, EdgeFlag.UNDIRECTED));
    }

    /**
     * Applies or removes blocked state on every edge record in a local relationship.
     *
     * @param nodeA   first node id
     * @param nodeB   second node id
     * @param blocked whether the relationship should be blocked
     * @return updated edge records
     */
    public List<Edge> updateRelationshipBlocked(final UUID nodeA, final UUID nodeB, final boolean blocked) {
        return updateRelationshipFlags(nodeA, nodeB, flags -> {
            if (blocked) {
                flags.add(EdgeFlag.BLOCKED);
            } else {
                flags.remove(EdgeFlag.BLOCKED);
            }
        });
    }

    /**
     * Applies or removes teleport traversal on every edge record in a local relationship.
     *
     * @param nodeA    first node id
     * @param nodeB    second node id
     * @param teleport whether the relationship should be teleport traversal
     * @return updated edge records
     */
    public List<Edge> updateRelationshipTeleport(final UUID nodeA, final UUID nodeB, final boolean teleport) {
        return updateRelationshipFlags(nodeA, nodeB, flags -> {
            if (teleport) {
                flags.add(EdgeFlag.TELEPORT);
            } else {
                flags.remove(EdgeFlag.TELEPORT);
            }
        });
    }

    private List<Edge> updateRelationshipFlags(final UUID nodeA, final UUID nodeB,
                                               final java.util.function.Consumer<Set<EdgeFlag>> change) {
        final List<Edge> edges = getEdgesBetween(nodeA, nodeB);
        final List<Edge> updated = new ArrayList<>(edges.size());
        for (final Edge edge : edges) {
            final Set<EdgeFlag> flags = mutableFlags(edge.flags());
            change.accept(flags);
            final Edge replacement = new Edge(edge.dbId(), edge.edgeId(), edge.source(), edge.target(), edge.cost(), flags);
            edgesById.put(replacement.edgeId(), replacement);
            final List<Edge> sourceEdges = adj.get(replacement.source());
            if (sourceEdges != null) {
                sourceEdges.replaceAll(existing -> existing.edgeId().equals(replacement.edgeId()) ? replacement : existing);
            }
            updated.add(replacement);
        }
        if (!updated.isEmpty()) {
            modCount++;
        }
        return List.copyOf(updated);
    }

    /**
     * Adds a new edge to the graph, either directed or undirected, based on the provided flags.
     *
     * @param source the {@link UUID} of the source node
     * @param target the {@link UUID} of the target node
     * @param cost   the cost of the edge
     * @param flags  the {@link Set} of {@link EdgeFlag}s that determine the characteristics of the edge;
     *               it must contain at least one flag and should specifically indicate whether the edge
     *               is directed or undirected
     * @return a {@link List} containing the created {@link Edge}(s); for directed edges, the list contains a single
     * edge, while for undirected edges, the list contains two edges
     * @throws IllegalArgumentException if the provided flags are null, empty, or do not clearly indicate
     *                                  whether the edge is directed or undirected
     */
    public List<Edge> addEdge(final UUID source, final UUID target, final double cost, final Set<EdgeFlag> flags) {
        if (flags == null || flags.isEmpty()) {
            throw new IllegalArgumentException("You need to specify at least one flag per Edge");
        }

        final boolean directed = flags.contains(EdgeFlag.DIRECTED) || flags.contains(EdgeFlag.INTER_GRAPH);
        final boolean undirected = flags.contains(EdgeFlag.UNDIRECTED) || flags.contains(EdgeFlag.TELEPORT);

        if (directed == undirected) {
            throw new IllegalArgumentException("To many flags that match both or neither, directed and undirected edges");
        }

        return directed
                ? List.of(addDirectedEdge(source, target, cost, flags))
                : addUndirectedEdge(source, target, cost, flags);
    }

    /**
     * Add a new directed edge target to the graph.
     *
     * @param source the id of the source node
     * @param target the id of the target node
     * @param cost   the cost of the edge
     * @return the created {@link Edge}
     */
    public Edge addDirectedEdge(final UUID source, final UUID target, final double cost) {
        return addDirectedEdge(source, target, cost, EnumSet.of(EdgeFlag.DIRECTED));
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
    public Edge addDirectedEdge(final UUID source, final UUID target, final double cost, final Set<EdgeFlag> flags) {
        final Set<EdgeFlag> editableFlags = getEditableFlags(flags);
        editableFlags.add(EdgeFlag.DIRECTED);
        return addStoredEdge(source, target, cost, editableFlags);
    }

    private Edge addStoredEdge(final UUID source, final UUID target, final double cost, final Set<EdgeFlag> flags) {
        requireNode(source);
        requireNode(target);
        final Edge edge = new Edge(-1, UUID.randomUUID(), source, target, cost, flags);
        edgesById.put(edge.edgeId(), edge);
        adj.computeIfAbsent(source, k -> new ArrayList<>()).add(edge);
        modCount++;

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
    public List<Edge> addUndirectedEdge(final UUID nodeA, final UUID nodeB, final double cost) {
        return addUndirectedEdge(nodeA, nodeB, cost, Set.of(EdgeFlag.UNDIRECTED));
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
    public List<Edge> addUndirectedEdge(final UUID nodeA, final UUID nodeB, final double cost, final Set<EdgeFlag> flags) {
        final Set<EdgeFlag> editableFlags = getEditableFlags(flags);
        editableFlags.remove(EdgeFlag.DIRECTED);
        editableFlags.add(EdgeFlag.UNDIRECTED);
        final Edge edgeOne = addStoredEdge(nodeA, nodeB, cost, editableFlags);
        final Edge edgeTwo = addStoredEdge(nodeB, nodeA, cost, editableFlags);

        return List.of(edgeOne, edgeTwo);
    }

    /**
     * Remove an edge from the graph.
     *
     * @param edge The edge to remove.
     */
    public void removeEdge(final Edge edge) {
        if (edge == null || !edgesById.containsKey(edge.edgeId())) {
            return;
        }
        edgesById.remove(edge.edgeId());
        final List<Edge> sourceEdges = adj.get(edge.source());
        if (sourceEdges != null) {
            sourceEdges.remove(edge);
        }
        final List<Edge> targetEdges = adj.get(edge.target());
        if (targetEdges != null) {
            targetEdges.remove(edge);
        }
        modCount++;
    }

    private Set<EdgeFlag> getEditableFlags(final Set<EdgeFlag> flags) {
        if (flags == null || flags.isEmpty()) {
            throw new IllegalArgumentException("Flags cannot be null or empty.");
        }
        return EnumSet.copyOf(flags);
    }

    private boolean isBetween(final Edge edge, final UUID nodeA, final UUID nodeB) {
        return edge.source().equals(nodeA) && edge.target().equals(nodeB)
                || edge.source().equals(nodeB) && edge.target().equals(nodeA);
    }

    private Set<EdgeFlag> relationshipFlags(final Set<EdgeFlag> flags, final EdgeFlag relationshipType) {
        final Set<EdgeFlag> result = mutableFlags(flags);
        result.remove(EdgeFlag.DIRECTED);
        result.remove(EdgeFlag.UNDIRECTED);
        result.remove(EdgeFlag.INTER_GRAPH);
        result.add(relationshipType);
        return result;
    }

    private Set<EdgeFlag> mutableFlags(final Set<EdgeFlag> flags) {
        return flags == null || flags.isEmpty() ? EnumSet.noneOf(EdgeFlag.class) : EnumSet.copyOf(flags);
    }

    /**
     * Get all neighbors of a node.
     *
     * @param nodeId The id of the node.
     * @return A {@link List} containing the neighbors.
     */
    public List<Edge> neighbors(final UUID nodeId) {
        return adj.getOrDefault(nodeId, List.of());
    }

    /**
     * Get an edge by its id.
     *
     * @param edgeId The id of the edge.
     * @return The {@link Edge} with the given id.
     */
    public Edge getEdgeById(final UUID edgeId) {
        return edgesById.get(edgeId);
    }

    /**
     * Check if a node exists.
     *
     * @param nodeId The id of the node.
     * @throws IllegalArgumentException If the node does not exist.
     */
    private void requireNode(final UUID nodeId) {
        if (!nodes.containsKey(nodeId)) {
            throw new IllegalArgumentException("Unknown node id: " + nodeId.toString());
        }
    }

    /**
     * Get the name of the graph.
     *
     * @return The name of the graph.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of the graph.
     *
     * @param name The new name of the graph.
     */
    public void setName(final String name) {
        this.name = name;
        modCount++;
    }

    /**
     * Get the database id of the graph.
     *
     * @return The database id of the graph.
     */
    public int getGraphId() {
        return graphId;
    }

    /**
     * Gets the structural modification count of the graph.
     *
     * @return modification count
     */
    public long getModCount() {
        return modCount;
    }

    /**
     * Creates a detached copy of this graph.
     *
     * @return copied graph preserving persistent ids and graph-local UUIDs
     */
    public Graph copy() {
        final Map<UUID, Node> copiedNodes = nodes.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                    final Node node = entry.getValue();
                    return new Node(node.dbId(), node.graphId(), node.x(), node.y(), node.z(), node.worldId(), node.flags());
                }));

        final Map<UUID, List<Edge>> copiedAdjacency = adj.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().stream()
                        .map(edge -> new Edge(edge.dbId(), edge.edgeId(), edge.source(), edge.target(), edge.cost(), edge.flags()))
                        .toList()));

        return new Graph(graphId, name, copiedNodes, copiedAdjacency);
    }
}
