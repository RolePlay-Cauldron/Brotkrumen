package com.github.roleplaycauldron.brotkrumen.graph;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Aggregates multiple graphs plus inter-graph edges.
 */
@SuppressWarnings({"PMD.GodClass", "PMD.TooManyMethods"})
public class GraphNetwork {

    private final Map<Integer, Graph> graphsByDbId;

    private final Map<NodeRef, List<InterGraphEdge>> outgoingInterEdges;

    private UnifiedGraph cachedUnifiedGraph;

    private long modCount;

    /**
     * Constructs a new instance of the GraphNetwork.
     */
    public GraphNetwork() {
        this.graphsByDbId = new HashMap<>();
        this.outgoingInterEdges = new HashMap<>();
        this.modCount = 0L;
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
        invalidateCache();
    }

    /**
     * Checks if a graph with the given ID is part of the network.
     *
     * @param graphId the ID of the graph to check
     * @return {@code true} if the graph is present, {@code false} otherwise
     */
    public boolean hasGraph(final int graphId) {
        return graphsByDbId.containsKey(graphId);
    }

    /**
     * Retrieves a graph by its ID from the network.
     *
     * @param graphId the ID of the graph to retrieve
     * @return the {@link Graph} instance, or {@code null} if not found
     */
    public Graph getGraph(final int graphId) {
        return graphsByDbId.get(graphId);
    }

    /**
     * Returns an unmodifiable collection of all graphs currently in the network.
     *
     * @return collection of graphs
     */
    public Collection<Graph> getGraphs() {
        return List.copyOf(graphsByDbId.values());
    }

    /**
     * Adds a directed inter-graph edge.
     *
     * @param source the source node reference
     * @param target the target node reference
     * @param cost   the cost associated with traversing the edge
     * @param flags  the edge flags specifying properties of the edge; if null or empty,
     *               default inter-graph directed flags are used
     * @return the created inter-graph edge
     */
    public InterGraphEdge addDirectedInterGraphEdge(final NodeRef source, final NodeRef target, final double cost,
                                                    final Set<EdgeFlag> flags) {
        final Set<EdgeFlag> normalizedFlags = directedInterGraphFlags(flags);

        final InterGraphEdge edge = new InterGraphEdge(UUID.randomUUID(), source, target, cost, normalizedFlags, true);
        addInterGraphEdge(edge);
        return edge;
    }

    /**
     * Adds a directed inter-graph edge with default flags.
     *
     * @param source the source node reference
     * @param target the target node reference
     * @param cost   the cost associated with traversing the edge
     * @return the created inter-graph edge
     */
    public InterGraphEdge addDirectedInterGraphEdge(final NodeRef source, final NodeRef target, final double cost) {
        return addDirectedInterGraphEdge(source, target, cost, null);
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
        invalidateCache();
    }

    /**
     * Adds an undirected inter-graph edge between two nodes with default flags.
     *
     * @param nodeA the reference for the first node in the undirected edge
     * @param nodeB the reference for the second node in the undirected edge
     * @param cost  the cost associated with traversing the edge
     * @return a list containing the two created inter-graph edges, one in each direction
     */
    public List<InterGraphEdge> addUndirectedInterGraphEdge(final NodeRef nodeA, final NodeRef nodeB,
                                                            final double cost) {
        return addUndirectedInterGraphEdge(nodeA, nodeB, cost, null);
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
        final Set<EdgeFlag> normalizedFlags = undirectedInterGraphFlags(flags);

        final InterGraphEdge edgeOne = new InterGraphEdge(UUID.randomUUID(), nodeA, nodeB, cost, normalizedFlags, true);
        final InterGraphEdge edgeTwo = new InterGraphEdge(UUID.randomUUID(), nodeB, nodeA, cost, normalizedFlags, true);

        addInterGraphEdge(edgeOne);
        addInterGraphEdge(edgeTwo);

        return List.of(edgeOne, edgeTwo);
    }

    private Set<EdgeFlag> directedInterGraphFlags(final Set<EdgeFlag> flags) {
        final Set<EdgeFlag> result = flags == null || flags.isEmpty()
                ? EnumSet.noneOf(EdgeFlag.class)
                : EnumSet.copyOf(flags);
        result.remove(EdgeFlag.UNDIRECTED);
        result.add(EdgeFlag.INTER_GRAPH);
        result.add(EdgeFlag.DIRECTED);
        return result;
    }

    private Set<EdgeFlag> undirectedInterGraphFlags(final Set<EdgeFlag> flags) {
        final Set<EdgeFlag> result = flags == null || flags.isEmpty()
                ? EnumSet.noneOf(EdgeFlag.class)
                : EnumSet.copyOf(flags);
        result.remove(EdgeFlag.DIRECTED);
        result.add(EdgeFlag.INTER_GRAPH);
        result.add(EdgeFlag.UNDIRECTED);
        return result;
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
        if (cachedUnifiedGraph != null) {
            return cachedUnifiedGraph;
        }
        final UnifiedGraph result = buildUnifiedGraph(null);
        this.cachedUnifiedGraph = result;
        return result;
    }

    /**
     * Constructs a temporary unified graph using teleport routing rules.
     *
     * @param rules teleport rules
     * @return unified graph containing only rule-enabled derived teleport routes
     */
    public UnifiedGraph toUnifiedGraph(final TeleportRules rules) {
        return rules == null ? toUnifiedGraph() : buildUnifiedGraph(rules);
    }

    private UnifiedGraph buildUnifiedGraph(final TeleportRules rules) {
        final Graph unified = new Graph("Unified graph network");
        final Map<NodeRef, UUID> unifiedIdByNodeRef = new HashMap<>();
        final Map<UUID, NodeRef> nodeRefByUnifiedId = new HashMap<>();

        copyNodesToUnifiedGraph(unified, unifiedIdByNodeRef, nodeRefByUnifiedId);
        copyLocalEdgesToUnifiedGraph(unified, unifiedIdByNodeRef);
        copyInterGraphEdgesToUnifiedGraph(unified, unifiedIdByNodeRef, rules);
        return new UnifiedGraph(unified, nodeRefByUnifiedId, unifiedIdByNodeRef);
    }

    private void copyNodesToUnifiedGraph(final Graph unified, final Map<NodeRef, UUID> unifiedIdByNodeRef,
                                         final Map<UUID, NodeRef> nodeRefByUnifiedId) {
        for (final Graph graph : graphsByDbId.values()) {
            for (final Node node : graph.getNodes()) {
                final NodeRef nodeRef = new NodeRef(graph.getGraphId(), node.graphId());
                final UUID unifiedNodeId = unifiedNodeId(nodeRef);
                unified.addNode(new Node(node.dbId(), unifiedNodeId, node.x(), node.y(), node.z(), node.worldId(),
                        node.flags()));
                unifiedIdByNodeRef.put(nodeRef, unifiedNodeId);
                nodeRefByUnifiedId.put(unifiedNodeId, nodeRef);
            }
        }
    }

    private void copyLocalEdgesToUnifiedGraph(final Graph unified, final Map<NodeRef, UUID> unifiedIdByNodeRef) {
        for (final Graph graph : graphsByDbId.values()) {
            for (final Edge edge : graph.getEdges()) {
                final UUID source = unifiedIdByNodeRef.get(new NodeRef(graph.getGraphId(), edge.source()));
                final UUID target = unifiedIdByNodeRef.get(new NodeRef(graph.getGraphId(), edge.target()));
                unified.addDirectedEdge(source, target, edge.cost(), edge.flags());
            }
        }
    }

    private void copyInterGraphEdgesToUnifiedGraph(final Graph unified, final Map<NodeRef, UUID> unifiedIdByNodeRef,
                                                   final TeleportRules rules) {
        for (final Collection<InterGraphEdge> edges : outgoingInterEdges.values()) {
            for (final InterGraphEdge edge : edges) {
                if (!edge.enabled()) {
                    continue;
                }
                if (isInterGraphTeleport(edge.flags()) && rules != null && !rules.isInterGraphTeleportEnabled()) {
                    continue;
                }
                final UUID source = unifiedIdByNodeRef.get(edge.source());
                final UUID target = unifiedIdByNodeRef.get(edge.target());
                unified.addDirectedEdge(source, target, edge.cost(), edge.flags());
            }
        }
    }

    private boolean isInterGraphTeleport(final Set<EdgeFlag> flags) {
        return flags.contains(EdgeFlag.TELEPORT) && flags.contains(EdgeFlag.INTER_GRAPH);
    }

    private void invalidateCache() {
        this.cachedUnifiedGraph = null;
        modCount++;
    }

    /**
     * Removes all outgoing inter-graph edges originating from the specified source node.
     *
     * @param source the source node reference
     */
    public void removeInterGraphEdges(final NodeRef source) {
        if (outgoingInterEdges.remove(source) != null) {
            invalidateCache();
        }
    }

    /**
     * Removes all inter-graph edges from a specific source node to a specific target node.
     *
     * @param source the source node reference
     * @param target the target node reference
     */
    public void removeInterGraphEdges(final NodeRef source, final NodeRef target) {
        final List<InterGraphEdge> edges = outgoingInterEdges.get(source);
        if (edges != null && edges.removeIf(edge -> edge.target().equals(target))) {
            if (edges.isEmpty()) {
                outgoingInterEdges.remove(source);
            }
            invalidateCache();
        }
    }

    /**
     * Checks whether the graphs in the network are connected to each other.
     * This implementation currently checks if there is at least one inter-graph edge
     * for each registered graph, or if it's the only graph in the network.
     * <p>
     * Disconnected graphs are automatically removed from the network.
     *
     * @return {@code true} if the network is likely connected, {@code false} otherwise.
     */
    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    public boolean removeDisconnectedGraphs() {
        if (graphsByDbId.size() <= 1) {
            return true;
        }

        final Set<Integer> connectedGraphs = new HashSet<>();
        for (final List<InterGraphEdge> edges : outgoingInterEdges.values()) {
            for (final InterGraphEdge edge : edges) {
                connectedGraphs.add(edge.source().graphDbId());
                connectedGraphs.add(edge.target().graphDbId());
            }
        }

        final List<Integer> orphans = graphsByDbId.keySet().stream()
                .filter(id -> !connectedGraphs.contains(id))
                .toList();

        for (final int orphanId : orphans) {
            removeGraph(orphanId);
        }

        return orphans.isEmpty();
    }

    /**
     * Removes a graph and all its associated inter-graph edges from the network.
     *
     * @param graphId the ID of the graph to remove
     */
    public void removeGraph(final int graphId) {
        graphsByDbId.remove(graphId);
        outgoingInterEdges.entrySet().removeIf(entry -> entry.getKey().graphDbId() == graphId);
        for (final List<InterGraphEdge> edges : outgoingInterEdges.values()) {
            edges.removeIf(edge -> edge.target().graphDbId() == graphId);
        }
        invalidateCache();
    }

    /**
     * Finds all nodes in the target graph that are reachable from outside via inter-graph edges.
     *
     * @param targetGraphId the ID of the target graph
     * @return a set of node references that are entry points to the graph
     */
    public Set<NodeRef> getGraphEntryPoints(final int targetGraphId) {
        final Set<NodeRef> entryPoints = new HashSet<>();
        for (final List<InterGraphEdge> edges : outgoingInterEdges.values()) {
            for (final InterGraphEdge edge : edges) {
                if (edge.enabled() && edge.target().graphDbId() == targetGraphId) {
                    entryPoints.add(edge.target());
                }
            }
        }
        return entryPoints;
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
     * Gets the structural modification count of the network.
     *
     * @return modification count
     */
    public long getModCount() {
        return modCount;
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
