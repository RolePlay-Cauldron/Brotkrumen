package com.github.roleplaycauldron.brotkrumen.storage.service;

import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.storage.database.Storage;
import com.github.roleplaycauldron.brotkrumen.storage.database.table.EdgeTable;
import com.github.roleplaycauldron.brotkrumen.storage.database.table.GraphTable;
import com.github.roleplaycauldron.brotkrumen.storage.database.table.NodeTable;
import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;

import java.util.Optional;
import java.util.Set;

public class GraphServiceImpl implements GraphService {

    private final WrappedLogger log;

    private final Storage storage;

    private final GraphTable graphTable;

    private final EdgeTable edgeTable;

    private final NodeTable nodeTable;

    public GraphServiceImpl(final WrappedLogger log, final Storage storage) {
        this.log = log;
        this.storage = storage;

        this.edgeTable = new EdgeTable(storage.getTablePrefix() + "_edge");
        this.nodeTable = new NodeTable(storage.getTablePrefix() + "_node");
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
