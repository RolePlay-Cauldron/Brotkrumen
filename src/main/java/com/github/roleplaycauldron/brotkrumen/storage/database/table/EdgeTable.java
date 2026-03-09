package com.github.roleplaycauldron.brotkrumen.storage.database.table;

import com.github.roleplaycauldron.brotkrumen.graph.Edge;

import java.sql.Connection;
import java.util.Set;

public class EdgeTable {

    private final String tableName;

    public EdgeTable(final String tableName) {
        this.tableName = tableName;
    }

    /* default */
    Set<Edge> getAllEdgesForGraph(final Connection con, final int graphId) {
        // ToDo
        return Set.of();
    }

    /* default */
    void deleteById(final Connection con, final int id) {
        // ToDo
    }

    /* default */
    void deleteByGraphId(final Connection con, final int graphId) {
        // ToDo
    }

    /**
     * Saves the specified edge to the database. If the edge does not already exist
     * (i.e., its edge ID is 0 or negative), it will insert a new record. Otherwise, it updates
     * the existing record corresponding to the edge ID.
     *
     * @param con  the database connection to be used for saving the edge
     * @param edge the edge object to be saved, containing edge details and an optional edge ID
     */
    /* default */
    void saveEdge(final Connection con, final Edge edge) {
        if (edge.dbId() <= 0) {
            createEdge(con, edge);
        } else {
            updateEdge(con, edge);
        }
    }

    private void updateEdge(final Connection con, final Edge edge) {
        // ToDo
    }

    private void createEdge(final Connection con, final Edge edge) {
        // ToDo
    }
}
