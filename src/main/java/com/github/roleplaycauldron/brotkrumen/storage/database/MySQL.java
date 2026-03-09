package com.github.roleplaycauldron.brotkrumen.storage.database;

import com.github.roleplaycauldron.brotkrumen.storage.StorageException;
import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.Configuration;

import java.sql.Connection;
import java.sql.SQLException;

public class MySQL implements BrotkrumenConnectionProvider {

    private static final String DEFAULT_DB_SECTION = "database";

    private final Configuration config;

    private final WrappedLogger log;

    private final String driverClassName;

    private HikariDataSource hikari;

    public MySQL(final Configuration config, final WrappedLogger log, final String driverClassName) {
        this.config = config;
        this.log = log;
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
            this.hikari = HikariDataSourceFactory.create(config.getConfigurationSection(DEFAULT_DB_SECTION), "", "");

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
