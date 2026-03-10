package com.github.roleplaycauldron.brotkrumen.storage.database.provider;

import com.github.roleplaycauldron.spellbook.database.ConnectionProvider;

import java.io.Closeable;

/**
 * Represents a provider for managing connections to a database.
 * This interface defines methods for opening, closing, and checking
 * the status of a connection pool. Implementing classes are expected
 * to handle the underlying connection mechanisms and provide a way
 * to retrieve database connections.
 */
public interface BrotkrumenConnectionProvider extends Closeable, ConnectionProvider {

    /**
     * Opens the underlying connection pool.
     */
    void open();

    /**
     * Returns whether the provider is closed.
     *
     * @return {@code true} if closed
     */
    boolean isClosed();
}
