package com.github.roleplaycauldron.brotkrumen.storage.database.provider;

import com.github.roleplaycauldron.brotkrumen.storage.StorageException;
import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Provides a connection to an SQLite database implementation of the {@link BrotkrumenConnectionProvider}.
 * This class is responsible for managing the lifecycle of an SQLite database connection, including
 * opening, closing, and retrieving connections safely.
 *
 * <p>Instances of this class are designed to ensure thread-safe access to the database connection
 * using a {@link ReadWriteLock}. A write lock is used to safely open and close the connection, while
 * a read lock is employed for operations that require an active connection.
 *
 * <p>The SQLite database file path and logging instance are specified at construction time. If the
 * database directory does not exist, it will be created.
 */
public class SQLite implements BrotkrumenConnectionProvider {

    private static final String DEFAULT_DATABASE_NAME = "brotkrumen.db";

    private final WrappedLogger log;

    private final String databasePath;

    private final ReadWriteLock stateLock;

    private boolean isOpen;

    /**
     * Constructs a new SQLite database connection provider.
     *
     * @param log          the logger instance used for logging operations
     * @param databasePath the file path to the SQLite database
     */
    public SQLite(final WrappedLogger log, final String databasePath) {
        this.log = log;
        this.databasePath = databasePath + DEFAULT_DATABASE_NAME;
        this.stateLock = new ReentrantReadWriteLock();
        this.isOpen = false;
    }

    @Override
    public void open() {
        stateLock.writeLock().lock();
        try {
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
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    @Override
    public Connection getConnection() {
        stateLock.readLock().lock();
        try {
            if (!isOpen) {
                throw new StorageException("The SQLite connection is not open!");
            }

            try {
                return DriverManager.getConnection("jdbc:sqlite:" + databasePath);
            } catch (final SQLException e) {
                throw new StorageException("Failed to get database connection", e);
            }
        } finally {
            stateLock.readLock().unlock();
        }
    }

    @Override
    public boolean isClosed() {
        stateLock.readLock().lock();
        try {
            return !isOpen;
        } finally {
            stateLock.readLock().unlock();
        }
    }

    @Override
    public void close() {
        stateLock.writeLock().lock();
        try {
            if (!isOpen) {
                log.error("Could not disconnect from the SQLite database, as it already was closed.");
                return;
            }

            this.isOpen = false;
        } finally {
            stateLock.writeLock().unlock();
        }
    }
}
