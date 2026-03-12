package com.github.roleplaycauldron.brotkrumen.storage.database;

import java.util.Locale;

/**
 * Represents a database engine type.
 * This enum includes support for MYSQL, MARIADB, and SQLITE engines.
 */
public enum Engine {
    /**
     * Specifies the MySQL database engine type.
     * Used to identify and differentiate MySQL as one of the supported database engines.
     */
    MYSQL,

    /**
     * Specifies the MariaDB database engine type.
     * Used to identify and differentiate MariaDB as one of the supported database engines.
     */
    MARIADB,

    /**
     * Specifies the SQLite database engine type.
     * Used to identify and differentiate SQLite as one of the supported database engines.
     */
    SQLITE;

    /**
     * Retrieves an {@code Engine} instance by its name.
     *
     * @param engine the name of the database engine in string format (e.g., "mysql", "mariadb", "sqlite").
     *               The comparison is case-insensitive.
     * @return the corresponding {@code Engine} instance for the provided name.
     * @throws IllegalArgumentException if the provided engine name does not match any supported database engine.
     */
    public static Engine getEngineByName(final String engine) {
        return switch (engine.toLowerCase(Locale.ROOT)) {
            case "mysql" -> MYSQL;
            case "mariadb" -> MARIADB;
            case "sqlite" -> SQLITE;
            default -> throw new IllegalArgumentException("Unknown database engine: " + engine);
        };
    }
}
