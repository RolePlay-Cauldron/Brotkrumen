package com.github.roleplaycauldron.brotkrumen.storage.database;

import com.github.roleplaycauldron.spellbook.database.ConnectionProvider;

import java.io.Closeable;
import java.sql.Connection;

public interface BrotkrumenConnectionProvider extends Closeable, ConnectionProvider {

    /**
     * Opens the underlying connection pool.
     */
    void open();

    /**
     * Gets a database connection.
     *
     * @return an open connection
     */
    @Override
    Connection getConnection();

    /**
     * Returns whether the provider is closed.
     *
     * @return {@code true} if closed
     */
    boolean isClosed();
}
