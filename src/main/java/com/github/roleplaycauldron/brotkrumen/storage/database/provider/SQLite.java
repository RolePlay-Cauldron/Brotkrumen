package com.github.roleplaycauldron.brotkrumen.storage.database.provider;

import com.github.roleplaycauldron.brotkrumen.storage.StorageException;
import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLite implements BrotkrumenConnectionProvider {

    private static final String DEFAULT_DATABASE_NAME = "brotkrumen.db";

    private final WrappedLogger log;

    private final String databasePath;

    private boolean isOpen;

    /**
     * Constructs a new SQLite database connection provider.
     *
     * @param log          the logger instance used for logging operations
     * @param databasePath the file path to the SQLite database
     */
    public SQLite(final WrappedLogger log, final String databasePath) {
        this.log = log;
        this.databasePath = databasePath;

        this.isOpen = false;
    }

    @Override
    public void open() {
        if (isOpen) {
            log.error("The SQLite connection is already open!");
            return;
        }

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (final ClassNotFoundException e) {
            log.error("SQLite JDBC Driver was not loaded!", e);
            return;
        }

        final File databaseFile = new File(databasePath);
        final File parent = databaseFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            log.error("Could not create SQLite database directory: " + parent.getAbsolutePath());
            return;
        }

        try (Connection ignored = DriverManager.getConnection("jdbc:sqlite:" + databasePath)) {
            this.isOpen = true;
            log.info("Connected to SQLite database at " + databasePath);
        } catch (final SQLException ex) {
            log.error("Could not connect to the SQLite database!", ex);
        }
    }

    @Override
    public Connection getConnection() {
        if (!isOpen) {
            throw new StorageException("The SQLite connection is not open!");
        }
        final Connection con;
        try {
            con = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
        } catch (final SQLException e) {
            throw new StorageException("Failed to get database connection", e);
        }
        return con;
    }

    @Override
    public boolean isClosed() {
        return !isOpen;
    }

    @Override
    public void close() {
        if (!isOpen) {
            log.error("Could not disconnect from the SQLite database, as it already was closed.");
            return;
        }
        this.isOpen = false;
    }
}
