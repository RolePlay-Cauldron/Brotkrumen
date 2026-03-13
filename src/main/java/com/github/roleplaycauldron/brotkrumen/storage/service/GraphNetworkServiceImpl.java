package com.github.roleplaycauldron.brotkrumen.storage.service;

import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.GraphNetwork;
import com.github.roleplaycauldron.brotkrumen.graph.InterGraphEdge;
import com.github.roleplaycauldron.brotkrumen.storage.database.Storage;
import com.github.roleplaycauldron.brotkrumen.storage.database.table.InterGraphEdgeTable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of the {@code GraphNetworkService} interface.
 */
public class GraphNetworkServiceImpl implements GraphNetworkService {

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
        final Set<Graph> allGraphs = graphService.getAllGraphs();
        final Set<InterGraphEdge> allEdges = interGraphEdgeTable.getAllEdges(storage.getProvider());

        final Map<Integer, Graph> graphMap = allGraphs.stream()
                .collect(Collectors.toMap(Graph::getGraphId, g -> g));

        final Map<Integer, Set<Integer>> adj = new HashMap<>();
        for (final int graphId : graphMap.keySet()) {
            adj.put(graphId, new HashSet<>());
        }

        for (final InterGraphEdge edge : allEdges) {
            final int source = edge.source().graphDbId();
            final int target = edge.target().graphDbId();
            if (adj.containsKey(source) && adj.containsKey(target)) {
                adj.get(source).add(target);
                adj.get(target).add(source);
            }
        }

        final List<GraphNetwork> networks = new ArrayList<>();
        final Set<Integer> visited = new HashSet<>();

        for (final int startId : adj.keySet()) {
            if (visited.add(startId)) {
                final Set<Integer> component = new HashSet<>();
                final Queue<Integer> queue = new LinkedList<>();
                queue.add(startId);
                component.add(startId);

                while (!queue.isEmpty()) {
                    final int currentId = queue.poll();
                    for (final int neighborId : adj.get(currentId)) {
                        if (visited.add(neighborId)) {
                            queue.add(neighborId);
                            component.add(neighborId);
                        }
                    }
                }

                final GraphNetwork network = new GraphNetwork();
                for (final int id : component) {
                    network.addGraph(graphMap.get(id));
                }

                for (final InterGraphEdge edge : allEdges) {
                    if (component.contains(edge.source().graphDbId())) {
                        network.addInterGraphEdge(edge);
                    }
                }
                networks.add(network);
            }
        }

        return networks;
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
