package com.github.roleplaycauldron.brotkrumen.storage.service;

import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.storage.database.Storage;
import com.github.roleplaycauldron.brotkrumen.storage.database.table.EdgeTable;
import com.github.roleplaycauldron.brotkrumen.storage.database.table.GraphTable;
import com.github.roleplaycauldron.brotkrumen.storage.database.table.NodeTable;

import java.util.Optional;
import java.util.Set;

/**
 * Implementation of the {@code GraphService} interface for managing graph-related operations.
 * This service interacts with the underlying storage infrastructure to perform CRUD operations
 * on graph entities.
 */
public class GraphServiceImpl implements GraphService {

    private final Storage storage;

    private final GraphTable graphTable;

    /**
     * Constructs a new instance of {@link GraphServiceImpl} with the specified storage.
     * Initializes the internal tables required for interacting with the underlying graph data.
     *
     * @param storage the storage implementation used to persist and retrieve graph-related data.
     *                This is expected to provide access to the database provider and table prefix.
     */
    public GraphServiceImpl(final Storage storage) {
        this.storage = storage;

        final EdgeTable edgeTable = new EdgeTable(storage.getTablePrefix() + "_edge");
        final NodeTable nodeTable = new NodeTable(storage.getTablePrefix() + "_node");
        this.graphTable = new GraphTable(storage.getTablePrefix() + "_graph", edgeTable, nodeTable);
    }

    @Override
    public Optional<Graph> loadGraphById(final int graphId) {
        return graphTable.findById(storage.getProvider(), graphId);
    }

    @Override
    public Optional<Graph> loadGraphByName(final String name) {
        return graphTable.findByName(storage.getProvider(), name);
    }

    @Override
    public Set<Graph> getAllGraphs() {
        return graphTable.getAllGraphs(storage.getProvider());
    }

    @Override
    public void saveGraph(final Graph graph) {
        graphTable.saveGraph(storage.getProvider(), graph);
    }

    @Override
    public void deleteGraph(final int graphId) {
        graphTable.deleteById(storage.getProvider(), graphId);
    }
}
