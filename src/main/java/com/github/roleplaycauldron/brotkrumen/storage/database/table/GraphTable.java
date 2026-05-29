package com.github.roleplaycauldron.brotkrumen.storage.database.table;

import com.github.roleplaycauldron.brotkrumen.graph.Edge;
import com.github.roleplaycauldron.brotkrumen.graph.Graph;
import com.github.roleplaycauldron.brotkrumen.graph.Node;
import com.github.roleplaycauldron.brotkrumen.storage.StorageException;
import com.github.roleplaycauldron.brotkrumen.storage.database.provider.BrotkrumenConnectionProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a table in a database that stores graph-related data, including nodes and edges.
 * This class provides methods for performing CRUD operations on graphs and their associated
 * data, such as retrieving, saving, and deleting graph entries.
 */
@SuppressWarnings("PMD.TooManyMethods")
public class GraphTable {

    private final String tableName;

    private final EdgeTable edgeTable;

    private final NodeTable nodeTable;

    /**
     * Constructs a GraphTable object, which represents a table in a database storing graph-related
     * data, including nodes and edges.
     *
     * @param tableName the name of the table representing the graph
     * @param edgeTable an instance of EdgeTable representing the table for storing edge data
     * @param nodeTable an instance of NodeTable representing the table for storing node data
     */
    public GraphTable(final String tableName, final EdgeTable edgeTable, final NodeTable nodeTable) {
        this.tableName = tableName;
        this.edgeTable = edgeTable;
        this.nodeTable = nodeTable;
    }

    /**
     * Finds a graph by its name from the database and returns it wrapped in an Optional.
     *
     * @param conProvider the provider for obtaining a database connection
     * @param name        the name of the graph to search for
     * @return an Optional containing the graph if found, or an empty Optional if no graph with the given name exists
     * @throws StorageException if a database access error occurs during the operation
     */
    public Optional<Graph> findByName(final BrotkrumenConnectionProvider conProvider, final String name) {
        final String sql = "SELECT `id`, `name` FROM `" + tableName + "` WHERE `name` = ?";

        try (Connection con = conProvider.getConnection();
             PreparedStatement statement = con.prepareStatement(sql)) {

            statement.setString(1, name);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                return Optional.of(mapGraph(con, resultSet));
            }
        } catch (final SQLException e) {
            throw new StorageException("Failed to find graph by name: " + name, e);
        }
    }

    /**
     * Retrieves a graph from the database by its unique ID.
     *
     * @param conProvider the provider for obtaining a database connection
     * @param graphId     the unique identifier of the graph to retrieve
     * @return an Optional containing the graph if found, or an empty Optional if no graph with the given ID exists
     * @throws StorageException if a database access error occurs during the operation
     */
    public Optional<Graph> findById(final BrotkrumenConnectionProvider conProvider, final int graphId) {
        final String sql = "SELECT `id`, `name` FROM `" + tableName + "` WHERE `id` = ?";

        try (Connection con = conProvider.getConnection();
             PreparedStatement statement = con.prepareStatement(sql)) {

            statement.setInt(1, graphId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                return Optional.of(mapGraph(con, resultSet));
            }
        } catch (final SQLException e) {
            throw new StorageException("Failed to find graph by graphId: " + graphId, e);
        }
    }

    /**
     * Retrieves all graphs from the database and returns them as a set.
     *
     * @param conProvider the provider for obtaining a database connection
     * @return a set of Graph objects, representing all the graphs stored in the database
     * @throws StorageException if a database access error occurs during the operation
     */
    public Set<Graph> getAllGraphs(final BrotkrumenConnectionProvider conProvider) {
        final String sql = "SELECT `id`, `name` FROM `" + tableName + "`";
        final Set<Graph> graphs = new LinkedHashSet<>();

        try (Connection con = conProvider.getConnection();
             PreparedStatement statement = con.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                graphs.add(mapGraph(con, resultSet));
            }

            return graphs;
        } catch (final SQLException e) {
            throw new StorageException("Failed to load all graphs", e);
        }
    }

    /**
     * Deletes a graph record from the database based on its unique identifier.
     *
     * @param conProvider the provider for obtaining a database connection
     * @param graphId     the unique identifier of the graph to delete
     * @throws StorageException if a database access error occurs during the operation
     */
    public void deleteById(final BrotkrumenConnectionProvider conProvider, final int graphId) {
        final String sql = "DELETE FROM `" + tableName + "` WHERE `id` = ?";

        try (Connection con = conProvider.getConnection();
             PreparedStatement statement = con.prepareStatement(sql)) {

            statement.setInt(1, graphId);
            statement.executeUpdate();
        } catch (final SQLException e) {
            throw new StorageException("Failed to delete graph with graphId " + graphId, e);
        }
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

    /**
     * Checks whether a graph name is already used by another graph.
     *
     * @param conProvider database connection provider
     * @param name        graph name to check
     * @param graphId     graph id that is allowed to keep this name
     * @return true if another graph uses the name
     */
    public boolean isNameUsedByOtherGraph(final BrotkrumenConnectionProvider conProvider, final String name,
                                          final int graphId) {
        final String sql = "SELECT `id` FROM `" + tableName + "` WHERE `name` = ? AND `id` <> ? LIMIT 1";

        try (Connection con = conProvider.getConnection();
             PreparedStatement statement = con.prepareStatement(sql)) {

            statement.setString(1, name);
            statement.setInt(2, graphId);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (final SQLException e) {
            throw new StorageException("Failed to check graph name uniqueness: " + name, e);
        }
    }

    private void updateGraph(final BrotkrumenConnectionProvider conProvider, final Graph graph) {
        final String updateGraphSql = "UPDATE `" + tableName + "` SET `name` = ? WHERE `id` = ?";

        try (Connection con = conProvider.getConnection()) {
            con.setAutoCommit(false);

            try {
                try (PreparedStatement statement = con.prepareStatement(updateGraphSql)) {
                    statement.setString(1, graph.getName());
                    statement.setInt(2, graph.getGraphId());
                    statement.executeUpdate();
                }

                syncNodes(con, graph);
                syncEdges(con, graph);

                con.commit();
            } catch (final SQLException ignored) {
                con.rollback();
            } finally {
                con.setAutoCommit(true);
            }
        } catch (final SQLException e) {
            throw new StorageException("Failed to update graph with id " + graph.getGraphId(), e);
        }
    }

    private void syncNodes(final Connection con, final Graph graph) {
        final Set<Node> dbNodes = nodeTable.getAllNodesForGraph(con, graph.getGraphId());

        final Map<UUID, Node> dbNodesByUuid = dbNodes.stream()
                .collect(java.util.stream.Collectors.toMap(Node::graphId, node -> node));

        final Map<UUID, Node> graphNodesByUuid = graph.getNodes().stream()
                .collect(java.util.stream.Collectors.toMap(Node::graphId, node -> node));

        for (final Node node : graph.getNodes()) {
            final Node existing = dbNodesByUuid.get(node.graphId());

            if (existing == null) {
                nodeTable.saveNode(con, graph.getGraphId(), node);
            } else {
                final Node nodeToUpdate = new Node(
                        existing.dbId(),
                        node.graphId(),
                        node.x(),
                        node.y(),
                        node.z(),
                        node.worldId(),
                        node.flags()
                );
                nodeTable.saveNode(con, graph.getGraphId(), nodeToUpdate);
            }
        }

        for (final Node dbNode : dbNodes) {
            if (!graphNodesByUuid.containsKey(dbNode.graphId())) {
                nodeTable.deleteById(con, dbNode.dbId());
            }
        }
    }

    private void syncEdges(final Connection con, final Graph graph) {
        final Set<Edge> dbEdges = edgeTable.getAllEdgesForGraph(con, graph.getGraphId());

        final Map<UUID, Edge> dbEdgesByUuid = dbEdges.stream()
                .collect(java.util.stream.Collectors.toMap(Edge::edgeId, edge -> edge));

        final Map<UUID, Edge> graphEdgesByUuid = graph.getEdges().stream()
                .collect(java.util.stream.Collectors.toMap(Edge::edgeId, edge -> edge));

        for (final Edge edge : graph.getEdges()) {
            final Edge existing = dbEdgesByUuid.get(edge.edgeId());

            if (existing == null) {
                edgeTable.saveEdge(con, graph.getGraphId(), edge);
            } else {
                final Edge edgeToUpdate = new Edge(
                        existing.dbId(),
                        edge.edgeId(),
                        edge.source(),
                        edge.target(),
                        edge.cost(),
                        edge.flags()
                );
                edgeTable.saveEdge(con, graph.getGraphId(), edgeToUpdate);
            }
        }

        for (final Edge dbEdge : dbEdges) {
            if (!graphEdgesByUuid.containsKey(dbEdge.edgeId())) {
                edgeTable.deleteById(con, dbEdge.dbId());
            }
        }
    }

    private void createGraph(final BrotkrumenConnectionProvider conProvider, final Graph graph) {
        final String insertGraphSql = "INSERT INTO `" + tableName + "` (`name`) VALUES (?)";

        try (Connection con = conProvider.getConnection()) {
            con.setAutoCommit(false);

            final int generatedGraphId;
            try (PreparedStatement statement = con.prepareStatement(insertGraphSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, graph.getName());
                statement.executeUpdate();

                try (ResultSet resultSet = statement.getGeneratedKeys()) {
                    if (!resultSet.next()) {
                        throw new StorageException("Failed to create graph. No generated key returned.");
                    }
                    generatedGraphId = resultSet.getInt(1);
                }
            }

            for (final Node node : graph.getNodes()) {
                nodeTable.saveNode(con, generatedGraphId, node);
            }

            for (final Edge edge : graph.getEdges()) {
                edgeTable.saveEdge(con, generatedGraphId, edge);
            }

            con.commit();
        } catch (final SQLException e) {
            throw new StorageException("Failed to create graph with name " + graph.getName(), e);
        }
    }

    private Graph mapGraph(final Connection con, final ResultSet resultSet) throws SQLException {
        final int graphId = resultSet.getInt("id");
        final String name = resultSet.getString("name");

        final Set<Node> nodes = nodeTable.getAllNodesForGraph(con, graphId);
        final Set<Edge> edges = edgeTable.getAllEdgesForGraph(con, graphId);

        final Map<UUID, Node> nodeMap = new HashMap<>();
        for (final Node node : nodes) {
            nodeMap.put(node.graphId(), node);
        }

        final Map<UUID, List<Edge>> adjacency = new HashMap<>();
        for (final Node node : nodes) {
            adjacency.put(node.graphId(), new java.util.ArrayList<>());
        }

        for (final Edge edge : edges) {
            adjacency.computeIfAbsent(edge.source(), ignored -> new java.util.ArrayList<>()).add(edge);
        }

        return new Graph(graphId, name, nodeMap, adjacency);
    }
}
