package com.github.roleplaycauldron.brotkrumen.storage.database.migrations;

import com.github.roleplaycauldron.spellbook.database.updater.DatabaseVersion;
import com.github.roleplaycauldron.spellbook.database.updater.builder.VersionListBuilder;

import java.util.List;

/**
 * This class provides a utility for generating MySQL database schema migrations
 * for a graph-based data model. It constructs and returns a list of database
 * version migration scripts tailored to the specified table prefix.
 */
public final class MySQLMigration {

    private MySQLMigration() {
    }

    /**
     * Builds a list of database versions with associated schema definitions and migration scripts
     * based on the provided table prefix. The generated queries include the creation of tables,
     * indexes, and foreign key constraints for graph, node, edge, and version data storage.
     *
     * @param tablePrefix a prefix to be used for the table names in the generated SQL queries,
     *                    allowing for data namespace isolation.
     * @return a list of {@code DatabaseVersion} objects that represent the database schemas
     * and migration scripts to be applied.
     */
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    public static List<DatabaseVersion> build(final String tablePrefix) {
        return new VersionListBuilder()
                .version(1)
                .addFirstStartupQuery(
                        "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "_graph` ("
                                + "`id` INT NOT NULL AUTO_INCREMENT, "
                                + "`name` VARCHAR(255) NOT NULL, "
                                + "PRIMARY KEY (`id`)"
                                + ")"
                )
                .addFirstStartupQuery(
                        "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "_node` ("
                                + "`id` INT NOT NULL AUTO_INCREMENT, "
                                + "`graph_id` INT NOT NULL, "
                                + "`node_id` CHAR(36) NOT NULL, "
                                + "`x` DOUBLE NOT NULL, "
                                + "`y` DOUBLE NOT NULL, "
                                + "`z` DOUBLE NOT NULL, "
                                + "`world_id` CHAR(36) NOT NULL, "
                                + "PRIMARY KEY (`id`), "
                                + "UNIQUE KEY `uk_" + tablePrefix + "_node_node_id` (`node_id`), "
                                + "KEY `idx_" + tablePrefix + "_node_graph_id` (`graph_id`), "
                                + "CONSTRAINT `fk_" + tablePrefix + "_node_graph` "
                                + "FOREIGN KEY (`graph_id`) REFERENCES `" + tablePrefix + "_graph` (`id`) "
                                + "ON DELETE CASCADE"
                                + ")"
                )
                .addFirstStartupQuery(
                        "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "_edge` ("
                                + "`id` INT NOT NULL AUTO_INCREMENT, "
                                + "`graph_id` INT NOT NULL, "
                                + "`edge_id` CHAR(36) NOT NULL, "
                                + "`source_node_id` CHAR(36) NOT NULL, "
                                + "`target_node_id` CHAR(36) NOT NULL, "
                                + "`cost` DOUBLE NOT NULL, "
                                + "`flags` TEXT NOT NULL, "
                                + "PRIMARY KEY (`id`), "
                                + "UNIQUE KEY `uk_" + tablePrefix + "_edge_edge_id` (`edge_id`), "
                                + "KEY `idx_" + tablePrefix + "_edge_graph_id` (`graph_id`), "
                                + "KEY `idx_" + tablePrefix + "_edge_source_node_id` (`source_node_id`), "
                                + "KEY `idx_" + tablePrefix + "_edge_target_node_id` (`target_node_id`), "
                                + "CONSTRAINT `fk_" + tablePrefix + "_edge_graph` "
                                + "FOREIGN KEY (`graph_id`) REFERENCES `" + tablePrefix + "_graph` (`id`) "
                                + "ON DELETE CASCADE, "
                                + "CONSTRAINT `fk_" + tablePrefix + "_edge_source_node` "
                                + "FOREIGN KEY (`source_node_id`) REFERENCES `" + tablePrefix + "_node` (`node_id`) "
                                + "ON DELETE CASCADE, "
                                + "CONSTRAINT `fk_" + tablePrefix + "_edge_target_node` "
                                + "FOREIGN KEY (`target_node_id`) REFERENCES `" + tablePrefix + "_node` (`node_id`) "
                                + "ON DELETE CASCADE"
                                + ")"
                )
                .addFirstStartupQuery(
                        "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "_inter_graph_edge` ("
                                + "`id` INT NOT NULL AUTO_INCREMENT, "
                                + "`edge_id` CHAR(36) NOT NULL, "
                                + "`source_graph_id` INT NOT NULL, "
                                + "`source_node_id` CHAR(36) NOT NULL, "
                                + "`target_graph_id` INT NOT NULL, "
                                + "`target_node_id` CHAR(36) NOT NULL, "
                                + "`cost` DOUBLE NOT NULL, "
                                + "`flags` TEXT NOT NULL, "
                                + "`enabled` BOOLEAN NOT NULL DEFAULT TRUE, "
                                + "PRIMARY KEY (`id`), "
                                + "UNIQUE KEY `uk_" + tablePrefix + "_inter_graph_edge_edge_id` (`edge_id`), "
                                + "KEY `idx_" + tablePrefix + "_inter_graph_edge_source_graph_id` (`source_graph_id`), "
                                + "KEY `idx_" + tablePrefix + "_inter_graph_edge_target_graph_id` (`target_graph_id`), "
                                + "CONSTRAINT `fk_" + tablePrefix + "_inter_graph_edge_source_graph` "
                                + "FOREIGN KEY (`source_graph_id`) REFERENCES `" + tablePrefix + "_graph` (`id`) ON DELETE CASCADE, "
                                + "CONSTRAINT `fk_" + tablePrefix + "_inter_graph_edge_target_graph` "
                                + "FOREIGN KEY (`target_graph_id`) REFERENCES `" + tablePrefix + "_graph` (`id`) ON DELETE CASCADE"
                                + ")"
                )
                .addFirstStartupQuery(
                        "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "_version` ("
                                + " `version_no` INTEGER NOT NULL,"
                                + " `applied_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP"
                                + ")"
                )
                .finishVersion().finish();
    }
}
