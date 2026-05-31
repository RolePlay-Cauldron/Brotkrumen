package com.github.roleplaycauldron.brotkrumen.storage.database.table;

import com.github.roleplaycauldron.brotkrumen.graph.Warp;
import com.github.roleplaycauldron.brotkrumen.storage.StorageException;
import com.github.roleplaycauldron.brotkrumen.storage.database.provider.BrotkrumenConnectionProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Handles persisted named warp definitions.
 */
public class WarpTable {

    private final String tableName;

    /**
     * Creates a warp table accessor.
     *
     * @param tableName database table name
     */
    public WarpTable(final String tableName) {
        this.tableName = tableName;
    }

    /**
     * Finds one warp by key.
     *
     * @param provider connection provider
     * @param key      warp key
     * @return matching warp
     */
    public Optional<Warp> findByKey(final BrotkrumenConnectionProvider provider, final String key) {
        final String sql = "SELECT `warp_key`, `target_node_id`, `cost`, `enabled`, `need_permission` "
                + "FROM `" + tableName + "` WHERE `warp_key` = ?";
        try (Connection con = provider.getConnection();
             PreparedStatement statement = con.prepareStatement(sql)) {
            statement.setString(1, key);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(mapWarp(resultSet)) : Optional.empty();
            }
        } catch (final SQLException e) {
            throw new StorageException("Failed to find warp with key " + key, e);
        }
    }

    /**
     * Loads all persisted warps.
     *
     * @param provider connection provider
     * @return persisted warps
     */
    public Set<Warp> getAllWarps(final BrotkrumenConnectionProvider provider) {
        return query(provider, "SELECT `warp_key`, `target_node_id`, `cost`, `enabled`, `need_permission` "
                + "FROM `" + tableName + "`", "Failed to load warps");
    }

    /**
     * Loads warps that target one node.
     *
     * @param provider     connection provider
     * @param targetNodeId node id
     * @return persisted warps for the target
     */
    public Set<Warp> findByTargetNodeId(final BrotkrumenConnectionProvider provider, final UUID targetNodeId) {
        final String sql = "SELECT `warp_key`, `target_node_id`, `cost`, `enabled`, `need_permission` "
                + "FROM `" + tableName + "` WHERE `target_node_id` = ?";
        final Set<Warp> warps = new LinkedHashSet<>();
        try (Connection con = provider.getConnection();
             PreparedStatement statement = con.prepareStatement(sql)) {
            statement.setString(1, targetNodeId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    warps.add(mapWarp(resultSet));
                }
            }
            return warps;
        } catch (final SQLException e) {
            throw new StorageException("Failed to load warps for target node " + targetNodeId, e);
        }
    }

    /**
     * Creates or updates a warp.
     *
     * @param provider connection provider
     * @param warp     warp to persist
     */
    public void saveWarp(final BrotkrumenConnectionProvider provider, final Warp warp) {
        if (findByKey(provider, warp.key()).isPresent()) {
            updateWarp(provider, warp);
            return;
        }
        createWarp(provider, warp);
    }

    /**
     * Deletes one warp by key.
     *
     * @param provider connection provider
     * @param key      warp key
     * @return true when a row was deleted
     */
    public boolean deleteByKey(final BrotkrumenConnectionProvider provider, final String key) {
        final String sql = "DELETE FROM `" + tableName + "` WHERE `warp_key` = ?";
        try (Connection con = provider.getConnection();
             PreparedStatement statement = con.prepareStatement(sql)) {
            statement.setString(1, key);
            return statement.executeUpdate() > 0;
        } catch (final SQLException e) {
            throw new StorageException("Failed to delete warp with key " + key, e);
        }
    }

    /**
     * Deletes warps targeting any requested node id.
     *
     * @param provider      connection provider
     * @param targetNodeIds target node ids
     * @return deleted row count
     */
    public int deleteByTargetNodeIds(final BrotkrumenConnectionProvider provider,
                                     final Collection<UUID> targetNodeIds) {
        if (targetNodeIds == null || targetNodeIds.isEmpty()) {
            return 0;
        }
        int deleted = 0;
        for (final UUID targetNodeId : targetNodeIds) {
            final String sql = "DELETE FROM `" + tableName + "` WHERE `target_node_id` = ?";
            try (Connection con = provider.getConnection();
                 PreparedStatement statement = con.prepareStatement(sql)) {
                statement.setString(1, targetNodeId.toString());
                deleted += statement.executeUpdate();
            } catch (final SQLException e) {
                throw new StorageException("Failed to delete warps for target node " + targetNodeId, e);
            }
        }
        return deleted;
    }

    private Set<Warp> query(final BrotkrumenConnectionProvider provider, final String sql, final String error) {
        final Set<Warp> warps = new LinkedHashSet<>();
        try (Connection con = provider.getConnection();
             PreparedStatement statement = con.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                warps.add(mapWarp(resultSet));
            }
            return warps;
        } catch (final SQLException e) {
            throw new StorageException(error, e);
        }
    }

    private Warp mapWarp(final ResultSet resultSet) throws SQLException {
        return new Warp(resultSet.getString("warp_key"),
                UUID.fromString(resultSet.getString("target_node_id")),
                resultSet.getDouble("cost"),
                resultSet.getBoolean("enabled"),
                resultSet.getBoolean("need_permission"));
    }

    private void createWarp(final BrotkrumenConnectionProvider provider, final Warp warp) {
        final String sql = "INSERT INTO `" + tableName + "` "
                + "(`warp_key`, `target_node_id`, `cost`, `enabled`, `need_permission`) VALUES (?, ?, ?, ?, ?)";
        write(provider, warp, sql, "Failed to create warp with key " + warp.key());
    }

    private void updateWarp(final BrotkrumenConnectionProvider provider, final Warp warp) {
        final String sql = "UPDATE `" + tableName + "` SET `target_node_id` = ?, `cost` = ?, `enabled` = ?, "
                + "`need_permission` = ? WHERE `warp_key` = ?";
        try (Connection con = provider.getConnection();
             PreparedStatement statement = con.prepareStatement(sql)) {
            statement.setString(1, warp.targetNodeId().toString());
            statement.setDouble(2, warp.cost());
            statement.setBoolean(3, warp.enabled());
            statement.setBoolean(4, warp.needPermission());
            statement.setString(5, warp.key());
            statement.executeUpdate();
        } catch (final SQLException e) {
            throw new StorageException("Failed to update warp with key " + warp.key(), e);
        }
    }

    private void write(final BrotkrumenConnectionProvider provider, final Warp warp, final String sql,
                       final String error) {
        try (Connection con = provider.getConnection();
             PreparedStatement statement = con.prepareStatement(sql)) {
            statement.setString(1, warp.key());
            statement.setString(2, warp.targetNodeId().toString());
            statement.setDouble(3, warp.cost());
            statement.setBoolean(4, warp.enabled());
            statement.setBoolean(5, warp.needPermission());
            statement.executeUpdate();
        } catch (final SQLException e) {
            throw new StorageException(error, e);
        }
    }
}
