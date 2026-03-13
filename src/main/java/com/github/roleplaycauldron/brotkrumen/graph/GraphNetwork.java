package com.github.roleplaycauldron.brotkrumen.graph;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Aggregates multiple graphs plus inter-graph edges.
 */
public class GraphNetwork {

    private final Map<Integer, Graph> graphsByDbId;

    private final Map<NodeRef, List<InterGraphEdge>> outgoingInterEdges;

    /**
     * Constructs a new instance of the GraphNetwork.
     */
    public GraphNetwork() {
        this.graphsByDbId = new HashMap<>();
        this.outgoingInterEdges = new HashMap<>();
    }

    /**
     * Registers a graph in the network.
     *
     * @param graph graph to add
     */
    public void addGraph(final Graph graph) {
        if (graph.getGraphId() < 0) {
            throw new IllegalArgumentException("Graph must have a persisted graph id for network usage.");
        }
        graphsByDbId.put(graph.getGraphId(), graph);
    }

    /**
     * Adds a directed inter-graph edge.
     *
     * @param edge edge to add
     */
    public void addInterGraphEdge(final InterGraphEdge edge) {
        requireNode(edge.source());
        requireNode(edge.target());

        final Set<EdgeFlag> flags = edge.flags() == null || edge.flags().isEmpty()
                ? EnumSet.of(EdgeFlag.INTER_GRAPH, EdgeFlag.DIRECTED)
                : edge.flags();

        final InterGraphEdge normalized = new InterGraphEdge(edge.dbId(), edge.edgeId(), edge.source(), edge.target(), edge.cost(), flags, edge.enabled());
        outgoingInterEdges.computeIfAbsent(normalized.source(), key -> new ArrayList<>()).add(normalized);
    }

    /**
     * Adds an undirected inter-graph edge between two nodes. This method creates two edges
     * with opposite directions between the specified nodes and identical properties,
     * ensuring the connection is undirected.
     *
     * @param nodeA the reference for the first node in the undirected edge
     * @param nodeB the reference for the second node in the undirected edge
     * @param cost  the cost associated with traversing the edge
     * @param flags an optional set of edge flags specifying properties of the edge;
     *              if null or empty, default flags indicating an undirected inter-graph edge are used
     * @return a list containing the two created inter-graph edges, one in each direction
     */
    public List<InterGraphEdge> addUndirectedInterGraphEdge(final NodeRef nodeA, final NodeRef nodeB, final double cost,
                                                            final Set<EdgeFlag> flags) {
        final Set<EdgeFlag> normalizedFlags = flags == null || flags.isEmpty()
                ? EnumSet.of(EdgeFlag.INTER_GRAPH, EdgeFlag.UNDIRECTED)
                : flags;

        final InterGraphEdge edgeOne = new InterGraphEdge(UUID.randomUUID(), nodeA, nodeB, cost, normalizedFlags, true);
        final InterGraphEdge edgeTwo = new InterGraphEdge(UUID.randomUUID(), nodeB, nodeA, cost, normalizedFlags, true);

        addInterGraphEdge(edgeOne);
        addInterGraphEdge(edgeTwo);

        return List.of(edgeOne, edgeTwo);
    }

    /**
     * Retrieves a list of outgoing inter-graph edges originating from the given source node.
     * This method fetches all inter-graph edges that have the specified node as their source,
     * or an empty list if none are found.
     *
     * @param source the reference to the source node whose outgoing inter-graph edges
     *               are to be retrieved
     * @return a list of {@link InterGraphEdge} instances representing the outgoing
     * inter-graph edges from the specified source node; returns an empty list
     * if no such edges exist
     */
    public List<InterGraphEdge> getOutgoingInterEdges(final NodeRef source) {
        return outgoingInterEdges.getOrDefault(source, List.of());
    }

    /**
     * Returns all inter-graph edges currently registered in this network.
     *
     * @return immutable list of all inter-graph edges
     */
    public List<InterGraphEdge> getInterGraphEdges() {
        return outgoingInterEdges.values().stream()
                .flatMap(Collection::stream)
                .toList();
    }

    /**
     * Resolves a node reference to its concrete node.
     *
     * @param nodeRef node reference
     * @return resolved node or {@code null} if unknown
     */
    public Node getNode(final NodeRef nodeRef) {
        final Graph graph = graphsByDbId.get(nodeRef.graphDbId());
        if (graph == null) {
            return null;
        }
        return graph.getNodeById(nodeRef.nodeId());
    }

    /**
     * Resolves a path of node references into concrete nodes.
     *
     * @param path node reference path
     * @return concrete node path (empty if any node cannot be resolved)
     */
    public List<Node> resolvePath(final List<NodeRef> path) {
        final List<Node> resolved = new ArrayList<>(path.size());
        for (final NodeRef nodeRef : path) {
            final Node node = getNode(nodeRef);
            if (node == null) {
                return List.of();
            }
            resolved.add(node);
        }
        return resolved;
    }

    /**
     * Constructs a temporary unified graph by combining multiple individual graphs and their relationships
     * from the current graph network. This method merges all nodes and edges from registered graphs, as well as
     * inter-graph edges, into a single unified graph structure.
     * <p>
     * Nodes from individual graphs are assigned unified node IDs, and mappings are created to allow
     * bi-directional lookups between original node references and their corresponding unified node IDs.
     * Edges (both internal to a graph and inter-graph) are added to establish connectivity in the unified graph.
     *
     * @return a {@code UnifiedGraph} instance that contains:
     * - a combined {@code Graph} of all nodes and edges in the network
     * - mappings from unified node IDs to original node references
     * - mappings from original node references to unified node IDs
     */
    public UnifiedGraph toUnifiedGraph() {
        final Graph unified = new Graph("Unified graph network");
        final Map<NodeRef, UUID> unifiedIdByNodeRef = new HashMap<>();
        final Map<UUID, NodeRef> nodeRefByUnifiedId = new HashMap<>();

        for (final Graph graph : graphsByDbId.values()) {
            for (final Node node : graph.getNodes()) {
                final NodeRef nodeRef = new NodeRef(graph.getGraphId(), node.graphId());
                final UUID unifiedNodeId = unifiedNodeId(nodeRef);
                unified.addNode(new Node(node.dbId(), unifiedNodeId, node.x(), node.y(), node.z(), node.worldId()));
                unifiedIdByNodeRef.put(nodeRef, unifiedNodeId);
                nodeRefByUnifiedId.put(unifiedNodeId, nodeRef);
            }
        }

        for (final Graph graph : graphsByDbId.values()) {
            for (final Edge edge : graph.getEdges()) {
                final UUID source = unifiedIdByNodeRef.get(new NodeRef(graph.getGraphId(), edge.source()));
                final UUID target = unifiedIdByNodeRef.get(new NodeRef(graph.getGraphId(), edge.target()));
                unified.addDirectedEdge(source, target, edge.cost(), edge.flags());
            }
        }

        for (final Collection<InterGraphEdge> edges : outgoingInterEdges.values()) {
            for (final InterGraphEdge edge : edges) {
                if (!edge.enabled()) {
                    continue;
                }
                final UUID source = unifiedIdByNodeRef.get(edge.source());
                final UUID target = unifiedIdByNodeRef.get(edge.target());
                unified.addDirectedEdge(source, target, edge.cost(), edge.flags());
            }
        }

        return new UnifiedGraph(unified, nodeRefByUnifiedId, unifiedIdByNodeRef);
    }

    private void requireNode(final NodeRef nodeRef) {
        final Graph graph = graphsByDbId.get(nodeRef.graphDbId());
        if (graph == null) {
            throw new IllegalArgumentException("Unknown graph id: " + nodeRef.graphDbId());
        }
        if (graph.getNodeById(nodeRef.nodeId()) == null) {
            throw new IllegalArgumentException("Unknown node in graph " + nodeRef.graphDbId() + ": " + nodeRef.nodeId());
        }
    }

    private UUID unifiedNodeId(final NodeRef nodeRef) {
        return UUID.nameUUIDFromBytes((nodeRef.graphDbId() + ":" + nodeRef.nodeId()).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Result of the temporary unified graph construction.
     *
     * @param graph              the graph used by existing path algorithms
     * @param nodeRefByUnifiedId reverse lookup from unified node id to original node reference
     * @param unifiedIdByNodeRef lookup from original node reference to unified node id
     */
    public record UnifiedGraph(Graph graph, Map<UUID, NodeRef> nodeRefByUnifiedId,
                               Map<NodeRef, UUID> unifiedIdByNodeRef) {

    }
}
