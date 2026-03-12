package com.github.roleplaycauldron.brotkrumen.storage.database.provider;

import com.github.roleplaycauldron.brotkrumen.storage.StorageException;
import com.github.roleplaycauldron.brotkrumen.storage.database.Engine;
import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

/**
 * Provides a MySQL-based implementation of the BrotkrumenConnectionProvider interface for managing database connections.
 * Responsible for opening, closing, and retrieving database connections using HikariCP as the connection pooling library.
 * This class supports multiple database engines (e.g., MySQL, MariaDB) and allows for configurable database connection
 * settings through a provided configuration section.
 */
public class MySQL implements BrotkrumenConnectionProvider {

    private final ConfigurationSection configSection;

    private final WrappedLogger log;

    private final Engine engine;

    private final String driverClassName;

    private HikariDataSource hikari;

    /**
     * Constructs a new MySQL database connection provider.
     *
     * @param configSection   the configuration section containing the database settings
     * @param log             the logger instance used for logging operations
     * @param engine          the database engine (e.g., MYSQL, MARIADB)
     * @param driverClassName the fully qualified class name of the JDBC driver
     */
    public MySQL(final ConfigurationSection configSection, final WrappedLogger log, final Engine engine, final String driverClassName) {
        this.configSection = configSection;
        this.log = log;
        this.engine = engine;
        this.driverClassName = driverClassName;
    }

    @Override
    public void open() {
        try {
            Class.forName(driverClassName);
        } catch (final ClassNotFoundException e) {
            log.error("JDBC Driver was not loaded: " + driverClassName, e);
            return;
        }
        if (isClosed()) {
            log.info("Connecting to database...");
            final String jdbcUrl = "jdbc:" + engine.name().toLowerCase(Locale.ROOT) + "://";
            this.hikari = HikariDataSourceFactory.create(log, configSection, jdbcUrl);

            if (!hikari.isClosed()) {
                log.info("Successfully connected to the server!");
                return;
            }
            log.error("Could not connect to the database server!");
            return;
        }
        log.error("The connection is already open!");
    }

    @Override
    public Connection getConnection() {
        final Connection con;
        try {
            con = hikari.getConnection();
        } catch (final SQLException e) {
            throw new StorageException("Failed to get database connection", e);
        }
        return con;
    }

    @Override
    public boolean isClosed() {
        return hikari == null || hikari.isClosed();
    }

    @Override
    public void close() {
        if (hikari != null) {
            log.info("Closing database connection");
            hikari.close();
            hikari = null;
            log.info("Successfully closed the database connection");
            return;
        }
        log.error("Could not disconnect from database, as it was already closed!");
    }
}
