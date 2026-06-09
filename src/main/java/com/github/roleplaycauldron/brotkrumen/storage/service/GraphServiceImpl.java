package com.github.roleplaycauldron.brotkrumen.storage.service;

import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.storage.StorageException;
import com.github.roleplaycauldron.brotkrumen.storage.database.Storage;
import com.github.roleplaycauldron.brotkrumen.storage.database.table.EdgeTable;
import com.github.roleplaycauldron.brotkrumen.storage.database.table.GraphTable;
import com.github.roleplaycauldron.brotkrumen.storage.database.table.NodeTable;
import com.github.roleplaycauldron.spellbook.core.cache.BiKeyLoadingCache;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Implementation of the {@code GraphService} interface for managing graph-related operations.
 * This service interacts with the underlying storage infrastructure to perform CRUD operations
 * on graph entities.
 */
public class GraphServiceImpl implements GraphService {

    private final Storage storage;

    private final BiKeyLoadingCache<Integer, String, Graph> graphCache;

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
        this.graphCache = new BiKeyLoadingCache<>(Graph::getGraphId,
                this::loadGraphById,
                Graph::getName,
                this::loadGraphByName);

        final EdgeTable edgeTable = new EdgeTable(storage.getTablePrefix() + "_edge");
        final NodeTable nodeTable = new NodeTable(storage.getTablePrefix() + "_node");
        this.graphTable = new GraphTable(storage.getTablePrefix() + "_graph", edgeTable, nodeTable);
    }

    /* default */ GraphServiceImpl(final Storage storage, final GraphTable graphTable) {
        this.storage = storage;
        this.graphCache = new BiKeyLoadingCache<>(Graph::getGraphId,
                this::loadGraphById,
                Graph::getName,
                this::loadGraphByName);
        this.graphTable = graphTable;
    }

    @Override
    public Optional<Graph> getGraphById(final int graphId) {
        return graphCache.getByFirstKey(graphId).map(Graph::copy);
    }

    @Override
    public Optional<Graph> getGraphByName(final String name) {
        return graphCache.getBySecondKey(name).map(Graph::copy);
    }

    @Override
    public Set<Graph> getAllGraphs() {
        if (graphCache.size() == 0) {
            loadAllGraphsToCache();
        }
        return graphCache.getAll().stream()
                .map(Graph::copy)
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));
    }

    private Set<Graph> loadAllGraphsToCache() {
        final Set<Graph> graphs = graphTable.getAllGraphs(storage.getProvider());
        graphCache.putAll(graphs);
        return graphs;
    }

    @Override
    public void saveGraph(final Graph graph) {
        if (graphTable.isNameUsedByOtherGraph(storage.getProvider(), graph.getName(), graph.getGraphId())) {
            throw new StorageException("A graph with the name '" + graph.getName() + "' already exists");
        }
        graphTable.saveGraph(storage.getProvider(), graph);
        if (graph.getGraphId() > 0) {
            graphCache.put(graph);
            return;
        }
        loadGraphByName(graph.getName()).ifPresent(graphCache::put);
    }

    @Override
    public void deleteGraph(final int graphId) {
        graphTable.deleteById(storage.getProvider(), graphId);
        graphCache.invalidateByFirstKey(graphId);
    }

    @Override
    public void invalidateCache() {
        graphCache.invalidateAll();
        loadAllGraphsToCache();
    }

    private Optional<Graph> loadGraphById(final int graphId) {
        return graphTable.findById(storage.getProvider(), graphId);
    }

    private Optional<Graph> loadGraphByName(final String name) {
        return graphTable.findByName(storage.getProvider(), name);
    }
}
