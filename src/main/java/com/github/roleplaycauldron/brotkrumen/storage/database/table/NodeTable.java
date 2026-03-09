package com.github.roleplaycauldron.brotkrumen.storage.database.table;

import com.github.roleplaycauldron.brotkrumen.graph.Node;

import java.sql.Connection;
import java.util.Set;

public class NodeTable {

    private final String tableName;

    public NodeTable(final String tableName) {
        this.tableName = tableName;
    }

    /* default */
    Set<Node> getAllNodesForGraph(final Connection con, final int graphId) {
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
     * Saves the specified node to the database. If the node does not already exist
     * (i.e., its node ID is 0 or negative), it will insert a new record. Otherwise, it updates
     * the existing record corresponding to the node ID.
     *
     * @param con  the database connection to be used for saving the node
     * @param node the node object to be saved, containing node details and an optional node ID
     */
    /* default */
    void saveNode(final Connection con, final Node node) {
        if (node.dbId() <= 0) {
            createNode(con, node);
        } else {
            updateNode(con, node);
        }
    }

    private void updateNode(final Connection con, final Node node) {
        // ToDo
    }

    private void createNode(final Connection con, final Node nodee) {
        // ToDo
    }
}
