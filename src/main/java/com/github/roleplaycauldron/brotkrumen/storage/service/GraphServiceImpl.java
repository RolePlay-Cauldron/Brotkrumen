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
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implementation of the {@code GraphService} interface for managing graph-related operations.
 * This service interacts with the underlying storage infrastructure to perform CRUD operations
 * on graph entities.
 */
public class GraphServiceImpl implements GraphService {

    private final Storage storage;

    private final BiKeyLoadingCache<Integer, String, Graph> graphCache;

    private final GraphTable graphTable;

    private final ReentrantLock cacheLock = new ReentrantLock();

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
        cacheLock.lock();
        try {
            return graphCache.getByFirstKey(graphId).map(Graph::copy);
        } finally {
            cacheLock.unlock();
        }
    }

    @Override
    public Optional<Graph> getGraphByName(final String name) {
        cacheLock.lock();
        try {
            return graphCache.getBySecondKey(name).map(Graph::copy);
        } finally {
            cacheLock.unlock();
        }
    }

    @Override
    public Set<Graph> getAllGraphs() {
        cacheLock.lock();
        try {
            if (graphCache.size() == 0) {
                loadAllGraphsToCache();
            }
            return graphCache.getAll().stream()
                    .map(Graph::copy)
                    .collect(java.util.stream.Collectors.toCollection(HashSet::new));
        } finally {
            cacheLock.unlock();
        }
    }

    private void loadAllGraphsToCache() {
        final Set<Graph> graphs = graphTable.getAllGraphs(storage.getProvider());
        graphCache.putAll(graphs);
    }

    @Override
    public void saveGraph(final Graph graph) {
        cacheLock.lock();
        try {
            if (graphTable.isNameUsedByOtherGraph(storage.getProvider(), graph.getName(), graph.getGraphId())) {
                throw new StorageException("A graph with the name '" + graph.getName() + "' already exists");
            }
            graphTable.saveGraph(storage.getProvider(), graph);
            if (graph.getGraphId() > 0) {
                graphCache.put(graph);
                return;
            }
            loadGraphByName(graph.getName()).ifPresent(graphCache::put);
        } finally {
            cacheLock.unlock();
        }
    }

    @Override
    public void deleteGraph(final int graphId) {
        cacheLock.lock();
        try {
            graphTable.deleteById(storage.getProvider(), graphId);
            graphCache.invalidateByFirstKey(graphId);
        } finally {
            cacheLock.unlock();
        }
    }

    @Override
    public void reloadGraphs() {
        cacheLock.lock();
        try {
            graphCache.invalidateAll();
            loadAllGraphsToCache();
        } finally {
            cacheLock.unlock();
        }
    }

    private Optional<Graph> loadGraphById(final int graphId) {
        return graphTable.findById(storage.getProvider(), graphId);
    }

    private Optional<Graph> loadGraphByName(final String name) {
        return graphTable.findByName(storage.getProvider(), name);
    }
}
