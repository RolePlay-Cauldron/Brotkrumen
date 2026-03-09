package com.github.roleplaycauldron.brotkrumen.storage.database.table;

import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.storage.database.provider.BrotkrumenConnectionProvider;

import java.util.Optional;
import java.util.Set;

public class GraphTable {

    private final String tableName;

    private final EdgeTable edgeTable;

    private final NodeTable nodeTable;

    public GraphTable(final String tableName, final EdgeTable edgeTable, final NodeTable nodeTable) {
        this.tableName = tableName;
        this.edgeTable = edgeTable;
        this.nodeTable = nodeTable;
    }

    public Optional<Graph> findByName(final BrotkrumenConnectionProvider conProvider, final String name) {
        // ToDo
        return Optional.empty();
    }

    public Optional<Graph> findById(final BrotkrumenConnectionProvider conProvider, final int name) {
        // ToDo
        return Optional.empty();
    }

    public Set<Graph> getAllGraphs(final BrotkrumenConnectionProvider conProvider) {
        // ToDo
        return Set.of();
    }

    public void deleteById(final BrotkrumenConnectionProvider conProvider, final int id) {
        // ToDo
    }

    /**
     * Saves the specified graph to the database. If the graph does not already exist
     * (i.e., its graph ID is null), it will insert a new record. Otherwise, it updates
     * the existing record corresponding to the graph ID.
     *
     * @param conProvider the database connection to be used for saving the graph
     * @param graph       the graph object to be saved, containing graph details and an optional graph ID
     */
    public void saveGraph(final BrotkrumenConnectionProvider conProvider, final Graph graph) {
        if (graph.getGraphId() <= 0) {
            createGraph(conProvider, graph);
        } else {
            updateGraph(conProvider, graph);
        }
    }

    private void updateGraph(final BrotkrumenConnectionProvider conProvider, final Graph graph) {
        // ToDo
    }

    private void createGraph(final BrotkrumenConnectionProvider conProvider, final Graph graph) {
        // ToDo
    }
}
