package com.github.roleplaycauldron.brotkrumen.storage.database;

import com.github.roleplaycauldron.brotkrumen.storage.StorageException;
import com.github.roleplaycauldron.brotkrumen.storage.database.provider.BrotkrumenConnectionProvider;
import com.github.roleplaycauldron.brotkrumen.storage.database.provider.MySQL;
import com.github.roleplaycauldron.brotkrumen.storage.database.provider.SQLite;
import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.github.roleplaycauldron.spellbook.database.updater.DatabaseUpdater;
import com.github.roleplaycauldron.spellbook.database.updater.DatabaseVersion;
import com.github.roleplaycauldron.spellbook.database.updater.DefaultVersionRepository;
import com.github.roleplaycauldron.spellbook.database.updater.builder.VersionBuilder;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.io.IOException;
import java.sql.SQLInput;
import java.util.List;

/**
 * The {@link Storage} class provides functionality for initializing, managing, and shutting down
 * a database connection for storage purposes. It supports different database engines and handles
 * table creation, database migration, and connection management.
 * <p>
 * The class uses configuration settings to determine the database engine, table prefixes, and
 * other connection parameters. Logging functionality is integrated via the provided logger factory.
 * <p>
 * Key responsibilities of this class include:
 * - Setting up database connections during startup
 * - Performing database schema migrations if necessary
 * - Managing the lifecycle of the database connection provider
 * - Providing access to the connection provider and table prefix
 */
public class Storage {

    private final LoggerFactory loggerFactory;

    private final WrappedLogger log;

    private final ConfigurationSection configSection;

    private final String tablePrefix;

    private final File dataFolder;

    private BrotkrumenConnectionProvider provider;

    /**
     * Constructs a new {@code Storage} instance with the specified logger factory and configuration section.
     * Initializes internal fields such as the logger and table prefix based on the provided configuration.
     *
     * @param loggerFactory        the {@link LoggerFactory} used to create loggers for the storage system
     * @param configurationSection the {@link ConfigurationSection} containing the configuration values,
     *                             including the table prefix for database operations
     * @param dataFolder           the root folder for the plugin's data files
     */
    public Storage(final LoggerFactory loggerFactory, final ConfigurationSection configurationSection, final File dataFolder) {
        this.loggerFactory = loggerFactory;
        this.log = loggerFactory.create(Storage.class);
        this.configSection = configurationSection;
        this.tablePrefix = configSection.getString("tablePrefix");
        this.dataFolder = dataFolder;
    }

    /**
     * Initializes the storage system by opening the database connection and performing necessary
     * setup operations such as creating tables and applying database updates. If the connection
     * provider is already open, a log message will indicate that the database is already connected.
     */
    public void initialize() {
        final String engineString = configSection.getString("storageMethod");
        if (engineString == null || engineString.isBlank()) {
            throw new IllegalArgumentException("Database engine configuration is missing");
        }
        if (provider == null || provider.isClosed()) {
            final Engine engine = Engine.getEngineByName(engineString);
            provider = getProvider(engine);
            provider.open();

            final DatabaseUpdater updater = DatabaseUpdater.builder()
                    .logger(loggerFactory.create(DatabaseUpdater.class))
                    .connectionProvider(provider)
                    .versionRepository(new DefaultVersionRepository(getDatabaseMigrationVersionList()))
                    .versionTable(tablePrefix + "_version",
                            "SELECT MAX(version_no) AS latest_version FROM " + tablePrefix + "_version`;",
                            "INSERT INTO `" + tablePrefix + "_version` (`version_no`) VALUES (?);")
                    .build();

            updater.firstStartup();
            return;
        }

        log.error("Database is already connected!");
    }

    @SuppressWarnings("PMD.AssignmentInOperand")
    private BrotkrumenConnectionProvider getProvider(final Engine engine) {
        BrotkrumenConnectionProvider provider = null;
        switch (engine) {
            case MYSQL ->
                    provider = new MySQL(configSection, loggerFactory.create(MySQL.class), engine, "com.mysql.cj.jdbc.Driver");
            case MARIADB ->
                    provider = new MySQL(configSection, loggerFactory.create(MySQL.class), engine, "org.mariadb.jdbc.Driver");
            case SQLITE -> provider = new SQLite(loggerFactory.create(SQLInput.class), dataFolder.getPath());
        }

        if (provider == null) {
            throw new StorageException("Unknown database engine");
        }
        return provider;
    }

    /**
     * Shuts down the storage by closing the connection provider if it is open.
     * <p>
     * This method checks whether the connection provider is already closed. If the provider
     * is not null and not closed, it attempts to close the provider. If an {@link IOException}
     * occurs during this process, it wraps the exception in a {@link StorageException} and throws it.
     *
     * @throws StorageException if an I/O error occurs while closing the provider
     */
    public void shutdown() {
        if (provider == null || provider.isClosed()) {
            return;
        }
        try {
            provider.close();
        } catch (final IOException e) {
            throw new StorageException("Failed to close database connection", e);
        }
    }

    /**
     * Retrieves the current connection provider for storage operations.
     *
     * @return the {@link BrotkrumenConnectionProvider} instance used for database connections
     */
    public BrotkrumenConnectionProvider getProvider() {
        return provider;
    }

    /**
     * Retrieves the table prefix used for database table naming within the storage system.
     *
     * @return the table prefix as a string
     */
    public String getTablePrefix() {
        return tablePrefix;
    }

    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    private List<DatabaseVersion> getDatabaseMigrationVersionList() {
        return new VersionBuilder(1, null)
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
                        "CREATE TABLE IF NOT EXISTS `" + tablePrefix + "_version` ("
                                + " `version_no` INTEGER NOT NULL,"
                                + " `applied_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP"
                                + ")"
                )
                .finishVersion().finish();
    }
}
