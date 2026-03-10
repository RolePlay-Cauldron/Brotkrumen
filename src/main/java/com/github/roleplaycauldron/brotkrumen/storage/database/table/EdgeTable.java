package com.github.roleplaycauldron.brotkrumen.storage.database.table;

import com.github.roleplaycauldron.brotkrumen.graph.Edge;
import com.github.roleplaycauldron.brotkrumen.graph.EdgeFlag;
import com.github.roleplaycauldron.brotkrumen.storage.StorageException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The {@code EdgeTable} class provides methods to manage and manipulate edges
 * stored in a relational database table. The class supports operations such as
 * retrieving, saving, deleting, and updating edges in the database.
 * <p>
 * Each edge is associated with a graph and stored in a table identified by a name.
 * The methods in this class use SQL queries to interact with the database and
 * handle edge-related operations.
 */
@SuppressWarnings("PMD.ShortVariable")
public class EdgeTable {

    private final String tableName;

    /**
     * Constructs an EdgeTable instance with the specified table name.
     *
     * @param tableName the name of the database table associated with this instance
     */
    public EdgeTable(final String tableName) {
        this.tableName = tableName;
    }

    /**
     * Retrieves all edges associated with a specified graph from the database.
     *
     * @param con     the database connection to be used for the query.
     * @param graphId the identifier of the graph whose edges are to be retrieved.
     * @return a set of {@link Edge} objects representing all edges of the specified graph.
     * @throws StorageException if an error occurs while querying the database.
     */
    /* default */
    Set<Edge> getAllEdgesForGraph(final Connection con, final int graphId) {
        final Set<Edge> edges = new HashSet<>();
        final String sql = "SELECT `id`, `edge_id`, `source_node_id`, `target_node_id`, `cost`, `flags` "
                + "FROM `" + tableName + "` WHERE `graph_id` = ?";

        try (PreparedStatement statement = con.prepareStatement(sql)) {
            statement.setInt(1, graphId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    edges.add(new Edge(
                            resultSet.getInt("id"),
                            UUID.fromString(resultSet.getString("edge_id")),
                            UUID.fromString(resultSet.getString("source_node_id")),
                            UUID.fromString(resultSet.getString("target_node_id")),
                            resultSet.getDouble("cost"),
                            parseFlags(resultSet.getString("flags"))
                    ));
                }
            }
        } catch (final SQLException e) {
            throw new StorageException("Failed to load edges for graph id " + graphId, e);
        }

        return edges;
    }

    /**
     * Deletes a record from the database table with the specified identifier.
     *
     * @param con the database connection to be used for executing the delete operation
     * @param id  the identifier of the record to be deleted
     * @throws StorageException if an error occurs while performing the delete operation
     */
    /* default */
    void deleteById(final Connection con, final int id) {
        final String sql = "DELETE FROM `" + tableName + "` WHERE `id` = ?";

        try (PreparedStatement statement = con.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (final SQLException e) {
            throw new StorageException("Failed to delete edge with id " + id, e);
        }
    }

    /**
     * Deletes all records associated with the specified graph identifier from the database table.
     *
     * @param con     the database connection to be used for executing the delete operation
     * @param graphId the identifier of the graph whose associated records are to be deleted
     * @throws StorageException if an error occurs while performing the delete operation
     */
    /* default */
    void deleteByGraphId(final Connection con, final int graphId) {
        final String sql = "DELETE FROM `" + tableName + "` WHERE `graph_id` = ?";

        try (PreparedStatement statement = con.prepareStatement(sql)) {
            statement.setInt(1, graphId);
            statement.executeUpdate();
        } catch (final SQLException e) {
            throw new StorageException("Failed to delete edges for graph id " + graphId, e);
        }
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
    void saveEdge(final Connection con, final int graphId, final Edge edge) {
        if (edge.dbId() <= 0) {
            createEdge(con, graphId, edge);
        } else {
            updateEdge(con, graphId, edge);
        }
    }

    private void updateEdge(final Connection con, final int graphId, final Edge edge) {
        final String sql = "UPDATE `" + tableName + "` "
                + "SET `graph_id` = ?, `edge_id` = ?, `source_node_id` = ?, `target_node_id` = ?, `cost` = ?, `flags` = ? "
                + "WHERE `id` = ?";

        try (PreparedStatement statement = con.prepareStatement(sql)) {
            statement.setInt(1, graphId);
            statement.setString(2, edge.edgeId().toString());
            statement.setString(3, edge.source().toString());
            statement.setString(4, edge.target().toString());
            statement.setDouble(5, edge.cost());
            statement.setString(6, serializeFlags(edge.flags()));
            statement.setInt(7, edge.dbId());
            statement.executeUpdate();
        } catch (final SQLException e) {
            throw new StorageException("Failed to update edge with id " + edge.dbId(), e);
        }
    }

    private void createEdge(final Connection con, final int graphId, final Edge edge) {
        final String sql = "INSERT INTO `" + tableName + "` "
                + "(`graph_id`, `edge_id`, `source_node_id`, `target_node_id`, `cost`, `flags`) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = con.prepareStatement(sql)) {
            statement.setInt(1, graphId);
            statement.setString(2, edge.edgeId().toString());
            statement.setString(3, edge.source().toString());
            statement.setString(4, edge.target().toString());
            statement.setDouble(5, edge.cost());
            statement.setString(6, serializeFlags(edge.flags()));
            statement.executeUpdate();
        } catch (final SQLException e) {
            throw new StorageException("Failed to create edge " + edge.edgeId(), e);
        }
    }

    private String serializeFlags(final Set<EdgeFlag> flags) {
        if (flags == null || flags.isEmpty()) {
            return "";
        }

        return flags.stream()
                .map(Enum::name)
                .collect(Collectors.joining(","));
    }

    private Set<EdgeFlag> parseFlags(final String flags) {
        if (flags == null || flags.isBlank()) {
            return EnumSet.noneOf(EdgeFlag.class);
        }

        return Arrays.stream(flags.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(EdgeFlag::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(EdgeFlag.class)));
    }
}
