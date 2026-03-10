package com.github.roleplaycauldron.brotkrumen.storage.database;

import com.github.roleplaycauldron.brotkrumen.storage.StorageException;
import com.github.roleplaycauldron.brotkrumen.storage.database.migrations.MariaDBMigration;
import com.github.roleplaycauldron.brotkrumen.storage.database.migrations.MySQLMigration;
import com.github.roleplaycauldron.brotkrumen.storage.database.migrations.SQLiteMigration;
import com.github.roleplaycauldron.brotkrumen.storage.database.provider.BrotkrumenConnectionProvider;
import com.github.roleplaycauldron.brotkrumen.storage.database.provider.MySQL;
import com.github.roleplaycauldron.brotkrumen.storage.database.provider.SQLite;
import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.github.roleplaycauldron.spellbook.database.updater.DatabaseUpdater;
import com.github.roleplaycauldron.spellbook.database.updater.DatabaseVersion;
import com.github.roleplaycauldron.spellbook.database.updater.DefaultVersionRepository;
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
                    .versionRepository(new DefaultVersionRepository(getDatabaseMigrationVersionList(engine)))
                    .versionTable(tablePrefix + "_version",
                            "SELECT MAX(version_no) AS latest_version FROM `" + tablePrefix + "_version`;",
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

    private List<DatabaseVersion> getDatabaseMigrationVersionList(final Engine engine) {
        return switch (engine) {
            case MYSQL -> MySQLMigration.build(tablePrefix);
            case MARIADB -> MariaDBMigration.build(tablePrefix);
            case SQLITE -> SQLiteMigration.build(tablePrefix);
        };
    }
}
