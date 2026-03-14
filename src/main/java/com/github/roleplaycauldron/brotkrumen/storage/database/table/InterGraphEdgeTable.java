package com.github.roleplaycauldron.brotkrumen.storage.database.table;

import com.github.roleplaycauldron.brotkrumen.graph.EdgeFlag;
import com.github.roleplaycauldron.brotkrumen.graph.InterGraphEdge;
import com.github.roleplaycauldron.brotkrumen.graph.NodeRef;
import com.github.roleplaycauldron.brotkrumen.storage.StorageException;
import com.github.roleplaycauldron.brotkrumen.storage.database.provider.BrotkrumenConnectionProvider;

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
 * Handles persistence operations for inter-graph edges.
 */
public class InterGraphEdgeTable {

    private final String tableName;

    /**
     * Constructs an instance of InterGraphEdgeTable with the specified table name.
     *
     * @param tableName the name of the table to associate with this instance
     */
    public InterGraphEdgeTable(final String tableName) {
        this.tableName = tableName;
    }

    /**
     * Retrieves all inter-graph edges stored in the database associated with this table.
     * Each edge represents a connection between nodes across potentially different graphs.
     *
     * @param provider the connection provider used to establish a connection to the database
     * @return a set of {@link InterGraphEdge} representing all edges retrieved from the database
     * @throws StorageException if an error occurs during database access or data retrieval
     */
    public Set<InterGraphEdge> getAllEdges(final BrotkrumenConnectionProvider provider) {
        final String sql = "SELECT `id`, `edge_id`, `source_graph_id`, `source_node_id`, `target_graph_id`, "
                + "`target_node_id`, `cost`, `flags`, `enabled` FROM `" + tableName + "`";
        final Set<InterGraphEdge> edges = new HashSet<>();

        try (Connection con = provider.getConnection()) {
            try (PreparedStatement statement = con.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    edges.add(new InterGraphEdge(
                            resultSet.getInt("id"),
                            UUID.fromString(resultSet.getString("edge_id")),
                            new NodeRef(resultSet.getInt("source_graph_id"), UUID.fromString(resultSet.getString("source_node_id"))),
                            new NodeRef(resultSet.getInt("target_graph_id"), UUID.fromString(resultSet.getString("target_node_id"))),
                            resultSet.getDouble("cost"),
                            parseFlags(resultSet.getString("flags")),
                            resultSet.getBoolean("enabled")
                    ));
                }
            }
        } catch (final SQLException e) {
            throw new StorageException("Failed to load inter-graph edges", e);
        }

        return edges;
    }

    /**
     * Deletes an entry from the database table associated with this instance based on the specified ID.
     *
     * @param provider the connection provider used to establish a connection to the database
     * @param dbId     the unique identifier of the entry to be deleted
     * @throws StorageException if an error occurs during the deletion or while accessing the database
     */
    public void deleteById(final BrotkrumenConnectionProvider provider, final int dbId) {
        final String sql = "DELETE FROM `" + tableName + "` WHERE `id` = ?";
        try (Connection con = provider.getConnection()) {
            try (PreparedStatement statement = con.prepareStatement(sql)) {
                statement.setInt(1, dbId);
                statement.executeUpdate();
            }
        } catch (final SQLException e) {
            throw new StorageException("Failed to delete inter-graph edge with dbId " + dbId, e);
        }
    }

    /**
     * Saves the given inter-graph edge to the database. If the edge does not already
     * exist in the database (indicated by a non-positive database ID), a new edge
     * is created. Otherwise, the existing edge is updated.
     *
     * @param provider the connection provider used to establish a connection to the database
     * @param edge     the inter-graph edge to save. The edge contains information such
     *                 as its unique identifier, source and target nodes, cost, flags,
     *                 and enabled status
     */
    public void saveEdge(final BrotkrumenConnectionProvider provider, final InterGraphEdge edge) {
        if (edge.dbId() <= 0) {
            createEdge(provider, edge);
            return;
        }
        updateEdge(provider, edge);
    }

    private void updateEdge(final BrotkrumenConnectionProvider provider, final InterGraphEdge edge) {
        final String sql = "UPDATE `" + tableName + "` SET `edge_id` = ?, `source_graph_id` = ?, `source_node_id` = ?, "
                + "`target_graph_id` = ?, `target_node_id` = ?, `cost` = ?, `flags` = ?, `enabled` = ? WHERE `id` = ?";

        try (Connection con = provider.getConnection()) {
            try (PreparedStatement statement = con.prepareStatement(sql)) {
                statement.setString(1, edge.edgeId().toString());
                statement.setInt(2, edge.source().graphDbId());
                statement.setString(3, edge.source().nodeId().toString());
                statement.setInt(4, edge.target().graphDbId());
                statement.setString(5, edge.target().nodeId().toString());
                statement.setDouble(6, edge.cost());
                statement.setString(7, serializeFlags(edge.flags()));
                statement.setBoolean(8, edge.enabled());
                statement.setInt(9, edge.dbId());
                statement.executeUpdate();
            }
        } catch (final SQLException e) {
            throw new StorageException("Failed to update inter-graph edge " + edge.edgeId(), e);
        }
    }

    private void createEdge(final BrotkrumenConnectionProvider provider, final InterGraphEdge edge) {
        final String sql = "INSERT INTO `" + tableName + "` (`edge_id`, `source_graph_id`, `source_node_id`, "
                + "`target_graph_id`, `target_node_id`, `cost`, `flags`, `enabled`) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = provider.getConnection()) {
            try (PreparedStatement statement = con.prepareStatement(sql)) {
                statement.setString(1, edge.edgeId().toString());
                statement.setInt(2, edge.source().graphDbId());
                statement.setString(3, edge.source().nodeId().toString());
                statement.setInt(4, edge.target().graphDbId());
                statement.setString(5, edge.target().nodeId().toString());
                statement.setDouble(6, edge.cost());
                statement.setString(7, serializeFlags(edge.flags()));
                statement.setBoolean(8, edge.enabled());
                statement.executeUpdate();
            }
        } catch (final SQLException e) {
            throw new StorageException("Failed to create inter-graph edge " + edge.edgeId(), e);
        }
    }

    private String serializeFlags(final Set<EdgeFlag> flags) {
        if (flags == null || flags.isEmpty()) {
            return "";
        }
        return flags.stream().map(Enum::name).collect(Collectors.joining(","));
    }

    private Set<EdgeFlag> parseFlags(final String flags) {
        if (flags == null || flags.isBlank()) {
            return EnumSet.noneOf(EdgeFlag.class);
        }

        return Arrays.stream(flags.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(EdgeFlag::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(EdgeFlag.class)));
    }
}
