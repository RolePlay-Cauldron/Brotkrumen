package com.github.roleplaycauldron.brotkrumen.storage.database.table;

import com.github.roleplaycauldron.brotkrumen.graph.Graph;

import java.sql.Connection;
import java.util.Optional;
import java.util.Set;

public class GraphTable {

    private final String tableName;

    public GraphTable(final String tableName) {
        this.tableName = tableName;
    }

    /* default */
    boolean hasAnyData(final Connection con) {
        // ToDo
        return false;
    }

    /* default */
    Optional<Integer> findIdByName(final Connection con, final String name) {
        // ToDo
        return Optional.empty();
    }

    /* default */
    String findNameById(final Connection con, final String name) {
        // ToDo
        return "";
    }

    /* default */
    Set<Graph> getAllGraphs(final Connection con) {
        // ToDo
        return Set.of();
    }

    /* default */
    void deleteById(final Connection con, final int id) {
        // ToDo
    }

    /**
     * Saves the specified graph to the database. If the graph does not already exist
     * (i.e., its graph ID is null), it will insert a new record. Otherwise, it updates
     * the existing record corresponding to the graph ID.
     *
     * @param con   the database connection to be used for saving the graph
     * @param graph the graph object to be saved, containing graph details and an optional graph ID
     */
    /* default */
    void saveGraph(final Connection con, final Graph graph) {
        if (graph.getGraphId() <= 0) {
            createGraph(con, graph);
        } else {
            updateGraph(con, graph);
        }
    }

    private void updateGraph(final Connection con, final Graph graph) {

    }

    private void createGraph(final Connection con, final Graph graph) {

    }
}
