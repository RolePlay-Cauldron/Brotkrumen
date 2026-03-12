package com.github.roleplaycauldron.brotkrumen.storage.database.table;

import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.storage.StorageException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The NodeTable class operates as an abstraction for interacting with a database table containing data
 * related to nodes in a graph. It provides various methods for retrieving, saving, updating, and deleting nodes
 * associated with specific graphs or IDs in a database.
 * <p>
 * This class is designed to work with SQL databases and utilizes {@link Connection}, {@link PreparedStatement},
 * and {@link ResultSet} for database operations. It also serves as a utility to manage nodes represented
 * by the {@link Node} class.
 */
@SuppressWarnings("PMD.ShortVariable")
public class NodeTable {

    private final String tableName;

    /**
     * Constructs a NodeTable instance with the specified table name.
     *
     * @param tableName the name of the database table associated with this instance
     */
    public NodeTable(final String tableName) {
        this.tableName = tableName;
    }

    /* default */
    Set<Node> getAllNodesForGraph(final Connection con, final int graphId) {
        final Set<Node> nodes = new HashSet<>();
        final String sql = "SELECT `id`, `node_id`, `x`, `y`, `z`, `world_id` FROM `" + tableName + "` WHERE `graph_id` = ?";

        try (PreparedStatement statement = con.prepareStatement(sql)) {
            statement.setInt(1, graphId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    nodes.add(new Node(
                            resultSet.getInt("id"),
                            UUID.fromString(resultSet.getString("node_id")),
                            resultSet.getDouble("x"),
                            resultSet.getDouble("y"),
                            resultSet.getDouble("z"),
                            UUID.fromString(resultSet.getString("world_id"))
                    ));
                }
            }
        } catch (final SQLException e) {
            throw new StorageException("Failed to load nodes for graph id " + graphId, e);
        }

        return nodes;
    }

    /**
     * Deletes the record with the specified ID from the database table associated with this instance.
     * <p>
     * This method executes a SQL DELETE statement targeting the table defined by the `tableName` field,
     * removing the row where the `id` matches the provided ID. If an error occurs during the execution
     * of the SQL statement, a {@link StorageException} is thrown.
     *
     * @param con the database connection to be used for the delete operation
     * @param id  the ID of the record to be deleted
     * @throws StorageException if a database access error occurs during the operation
     */
    /* default */
    void deleteById(final Connection con, final int id) {
        final String sql = "DELETE FROM `" + tableName + "` WHERE `id` = ?";

        try (PreparedStatement statement = con.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (final SQLException e) {
            throw new StorageException("Failed to delete node with id " + id, e);
        }
    }

    /**
     * Deletes all records in the database table associated with the specified graph ID.
     * <p>
     * This method executes a SQL DELETE operation on the table defined by the
     * `tableName` field, removing all rows where the `graph_id` matches the provided
     * graph ID. If an error occurs during the execution of the SQL statement, a
     * {@link StorageException} is thrown.
     *
     * @param con     the database connection to be used for the delete operation
     * @param graphId the ID of the graph whose associated records should be deleted
     * @throws StorageException if a database access error occurs during operation
     */
    /* default */
    void deleteByGraphId(final Connection con, final int graphId) {
        final String sql = "DELETE FROM `" + tableName + "` WHERE `graph_id` = ?";

        try (PreparedStatement statement = con.prepareStatement(sql)) {
            statement.setInt(1, graphId);
            statement.executeUpdate();
        } catch (final SQLException e) {
            throw new StorageException("Failed to delete nodes for graph id " + graphId, e);
        }
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
    void saveNode(final Connection con, final int graphId, final Node node) {
        if (node.dbId() <= 0) {
            createNode(con, graphId, node);
        } else {
            updateNode(con, graphId, node);
        }
    }

    private void updateNode(final Connection con, final int graphId, final Node node) {
        final String sql = "UPDATE `" + tableName + "` "
                + "SET `graph_id` = ?, `node_id` = ?, `x` = ?, `y` = ?, `z` = ?, `world_id` = ? "
                + "WHERE `id` = ?";

        try (PreparedStatement statement = con.prepareStatement(sql)) {
            statement.setInt(1, graphId);
            statement.setString(2, node.graphId().toString());
            statement.setDouble(3, node.x());
            statement.setDouble(4, node.y());
            statement.setDouble(5, node.z());
            statement.setString(6, node.worldId().toString());
            statement.setInt(7, node.dbId());
            statement.executeUpdate();
        } catch (final SQLException e) {
            throw new StorageException("Failed to update node with id " + node.dbId(), e);
        }
    }

    private void createNode(final Connection con, final int graphId, final Node node) {
        final String sql = "INSERT INTO `" + tableName + "` (`graph_id`, `node_id`, `x`, `y`, `z`, `world_id`) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = con.prepareStatement(sql)) {
            statement.setInt(1, graphId);
            statement.setString(2, node.graphId().toString());
            statement.setDouble(3, node.x());
            statement.setDouble(4, node.y());
            statement.setDouble(5, node.z());
            statement.setString(6, node.worldId().toString());
            statement.executeUpdate();
        } catch (final SQLException e) {
            throw new StorageException("Failed to create node " + node.graphId(), e);
        }
    }
}
