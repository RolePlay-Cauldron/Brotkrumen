package com.github.roleplaycauldron.brotkrumen.storage.service;

import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork;
import com.github.roleplaycauldron.brotkrumen.graph.InterGraphEdge;
import com.github.roleplaycauldron.brotkrumen.storage.database.Storage;
import com.github.roleplaycauldron.brotkrumen.storage.database.table.InterGraphEdgeTable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementation of the {@code GraphNetworkService} interface.
 */
public class GraphNetworkServiceImpl implements GraphNetworkService {

    private static final int MINIMUM_NETWORK_GRAPH_COUNT = 2;

    private final Storage storage;

    private final GraphService graphService;

    private final InterGraphEdgeTable interGraphEdgeTable;

    /**
     * Constructs a new instance of {@link GraphNetworkServiceImpl}.
     *
     * @param storage      the storage implementation
     * @param graphService the graph service to load individual graphs
     */
    public GraphNetworkServiceImpl(final Storage storage, final GraphService graphService) {
        this(storage, graphService, new InterGraphEdgeTable(storage.getTablePrefix() + "_inter_graph_edge"));
    }

    /**
     * Constructs a new instance of {@link GraphNetworkServiceImpl} with a specific table.
     *
     * @param storage             the storage implementation
     * @param graphService        the graph service to load individual graphs
     * @param interGraphEdgeTable the table for inter-graph edges
     */
    public GraphNetworkServiceImpl(final Storage storage, final GraphService graphService,
                                   final InterGraphEdgeTable interGraphEdgeTable) {
        this.storage = storage;
        this.graphService = graphService;
        this.interGraphEdgeTable = interGraphEdgeTable;
    }

    @Override
    public Collection<GraphNetwork> loadGraphNetworks() {
        final Set<Graph> graphs = graphService.getAllGraphs();
        final Set<InterGraphEdge> edges = interGraphEdgeTable.getAllEdges(storage.getProvider());

        final Map<Integer, Graph> graphById = graphs.stream()
                .collect(Collectors.toMap(Graph::getGraphId, Function.identity()));

        final Map<Integer, Set<Integer>> adjacency = buildAdjacency(graphById.keySet(), edges);

        final List<GraphNetwork> networks = new ArrayList<>();
        final Set<Integer> visited = new HashSet<>();

        for (final Integer graphId : graphById.keySet()) {
            if (visited.contains(graphId)) {
                continue;
            }

            final Set<Integer> component = collectComponent(graphId, adjacency, visited);
            if (component.size() >= MINIMUM_NETWORK_GRAPH_COUNT) {
                networks.add(buildNetwork(component, graphById, edges));
            }
        }

        return networks;
    }

    private Map<Integer, Set<Integer>> buildAdjacency(
            final Set<Integer> graphIds,
            final Set<InterGraphEdge> edges
    ) {
        final Map<Integer, Set<Integer>> adjacency = new HashMap<>();

        for (final Integer graphId : graphIds) {
            adjacency.put(graphId, new HashSet<>());
        }

        for (final InterGraphEdge edge : edges) {
            final int source = edge.source().graphDbId();
            final int target = edge.target().graphDbId();

            if (!adjacency.containsKey(source) || !adjacency.containsKey(target)) {
                continue;
            }

            adjacency.get(source).add(target);
            adjacency.get(target).add(source);
        }

        return adjacency;
    }

    private Set<Integer> collectComponent(
            final Integer startId,
            final Map<Integer, Set<Integer>> adjacency,
            final Set<Integer> visited
    ) {
        final Set<Integer> component = new HashSet<>();
        final Deque<Integer> stack = new ArrayDeque<>();

        stack.push(startId);
        visited.add(startId);

        while (!stack.isEmpty()) {
            final Integer current = stack.pop();
            component.add(current);

            for (final Integer neighbor : adjacency.get(current)) {
                if (visited.add(neighbor)) {
                    stack.push(neighbor);
                }
            }
        }

        return component;
    }

    private GraphNetwork buildNetwork(
            final Set<Integer> component,
            final Map<Integer, Graph> graphById,
            final Set<InterGraphEdge> edges
    ) {
        final GraphNetwork network = new GraphNetwork();

        for (final Integer graphId : component) {
            network.addGraph(graphById.get(graphId));
        }

        for (final InterGraphEdge edge : edges) {
            final int source = edge.source().graphDbId();
            final int target = edge.target().graphDbId();

            if (component.contains(source) && component.contains(target)) {
                network.addInterGraphEdge(edge);
            }
        }

        return network;
    }

    @Override
    public void saveInterGraphEdges(final GraphNetwork network) {
        final Set<InterGraphEdge> dbEdges = interGraphEdgeTable.getAllEdges(storage.getProvider());
        final Map<UUID, InterGraphEdge> dbById = dbEdges.stream()
                .collect(Collectors.toMap(InterGraphEdge::edgeId, edge -> edge));
        final Map<UUID, InterGraphEdge> networkById = network.getInterGraphEdges().stream()
                .collect(Collectors.toMap(InterGraphEdge::edgeId, edge -> edge, (left, right) -> left));

        for (final InterGraphEdge edge : networkById.values()) {
            final InterGraphEdge existing = dbById.get(edge.edgeId());
            if (existing == null) {
                interGraphEdgeTable.saveEdge(storage.getProvider(), edge);
                continue;
            }

            interGraphEdgeTable.saveEdge(storage.getProvider(), new InterGraphEdge(
                    existing.dbId(),
                    edge.edgeId(),
                    edge.source(),
                    edge.target(),
                    edge.cost(),
                    edge.flags(),
                    edge.enabled()
            ));
        }

        final Set<Integer> networkGraphIds = network.getGraphs().stream()
                .map(Graph::getGraphId)
                .collect(Collectors.toSet());

        for (final InterGraphEdge dbEdge : dbEdges) {
            final boolean belongsToNetwork = networkGraphIds.contains(dbEdge.source().graphDbId())
                    || networkGraphIds.contains(dbEdge.target().graphDbId());

            if (belongsToNetwork && !networkById.containsKey(dbEdge.edgeId())) {
                interGraphEdgeTable.deleteById(storage.getProvider(), dbEdge.dbId());
            }
        }
    }

    @Override
    public void deleteInterGraphEdges(final GraphNetwork network) {
        final Set<InterGraphEdge> dbEdges = interGraphEdgeTable.getAllEdges(storage.getProvider());
        final Set<UUID> networkEdgeIds = network.getInterGraphEdges().stream()
                .map(InterGraphEdge::edgeId)
                .collect(Collectors.toSet());

        for (final InterGraphEdge dbEdge : dbEdges) {
            if (networkEdgeIds.contains(dbEdge.edgeId())) {
                interGraphEdgeTable.deleteById(storage.getProvider(), dbEdge.dbId());
            }
        }
    }
}
