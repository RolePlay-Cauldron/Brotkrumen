package com.github.roleplaycauldron.brotkrumen.storage.database.provider;

import com.github.roleplaycauldron.spellbook.database.ConnectionProvider;

import java.io.Closeable;

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
