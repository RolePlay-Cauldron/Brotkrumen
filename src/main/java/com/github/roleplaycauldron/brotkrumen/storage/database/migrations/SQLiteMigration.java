package com.github.roleplaycauldron.brotkrumen.storage.database.migrations;

import com.github.roleplaycauldron.spellbook.database.updater.DatabaseVersion;
import com.github.roleplaycauldron.spellbook.database.updater.builder.VersionListBuilder;

import java.util.List;

/**
 * The SQLiteMigration class is responsible for generating a set of database migration
 * queries tailored for an SQLite database. It creates necessary tables, indexes, and
 * relationships required to manage a graph structure, including nodes, edges, and their
 * associations. The class utilizes a VersionListBuilder to define versioned queries that
 * are executed during the database initialization or upgrade process.
 */
public final class SQLiteMigration {

    private SQLiteMigration() {

    }

    /**
     * Constructs a list of {@link DatabaseVersion} objects by defining the database schema
     * for a SQLite-based migration system. The method creates necessary tables, indices,
     * and versioning information based on the provided table prefix.
     *
     * @param tablePrefix the prefix to be applied to the names of all database tables and indices.
     *                    This allows multiple sets of tables to coexist within the same database.
     * @return a list of {@link DatabaseVersion} objects representing the defined schema and
     * migration versions.
     */
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    public static List<DatabaseVersion> build(final String tablePrefix) {
        return new VersionListBuilder()
                .version(1)
                .addFirstStartupQuery(
                        "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "_graph` ("
                                + "`id` INTEGER PRIMARY KEY AUTOINCREMENT, "
                                + "`name` TEXT NOT NULL"
                                + ")"
                )
                .addFirstStartupQuery(
                        "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "_node` ("
                                + "`id` INTEGER PRIMARY KEY AUTOINCREMENT, "
                                + "`graph_id` INTEGER NOT NULL, "
                                + "`node_id` CHAR(36) NOT NULL UNIQUE, "
                                + "`x` DOUBLE NOT NULL, "
                                + "`y` DOUBLE NOT NULL, "
                                + "`z` DOUBLE NOT NULL, "
                                + "`world_id` CHAR(36) NOT NULL, "
                                + "FOREIGN KEY (`graph_id`) REFERENCES `" + tablePrefix + "_graph` (`id`) ON DELETE CASCADE"
                                + ")"
                )
                .addFirstStartupQuery(
                        "CREATE INDEX IF NOT EXISTS `idx_" + tablePrefix + "_node_graph_id` "
                                + "ON `" + tablePrefix + "_node` (`graph_id`)"
                )
                .addFirstStartupQuery(
                        "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "_edge` ("
                                + "`id` INTEGER PRIMARY KEY AUTOINCREMENT, "
                                + "`graph_id` INTEGER NOT NULL, "
                                + "`edge_id` CHAR(36) NOT NULL UNIQUE, "
                                + "`source_node_id` CHAR(36) NOT NULL, "
                                + "`target_node_id` CHAR(36) NOT NULL, "
                                + "`cost` DOUBLE NOT NULL, "
                                + "`flags` TEXT NOT NULL, "
                                + "FOREIGN KEY (`graph_id`) REFERENCES `" + tablePrefix + "_graph` (`id`) ON DELETE CASCADE, "
                                + "FOREIGN KEY (`source_node_id`) REFERENCES `" + tablePrefix + "_node` (`node_id`) ON DELETE CASCADE, "
                                + "FOREIGN KEY (`target_node_id`) REFERENCES `" + tablePrefix + "_node` (`node_id`) ON DELETE CASCADE"
                                + ")"
                )
                .addFirstStartupQuery(
                        "CREATE INDEX IF NOT EXISTS `idx_" + tablePrefix + "_edge_graph_id` "
                                + "ON `" + tablePrefix + "_edge` (`graph_id`)"
                )
                .addFirstStartupQuery(
                        "CREATE INDEX IF NOT EXISTS `idx_" + tablePrefix + "_edge_source_node_id` "
                                + "ON `" + tablePrefix + "_edge` (`source_node_id`)"
                )
                .addFirstStartupQuery(
                        "CREATE INDEX IF NOT EXISTS `idx_" + tablePrefix + "_edge_target_node_id` "
                                + "ON `" + tablePrefix + "_edge` (`target_node_id`)"
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
