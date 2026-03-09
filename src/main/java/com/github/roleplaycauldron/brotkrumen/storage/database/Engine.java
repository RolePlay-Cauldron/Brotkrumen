package com.github.roleplaycauldron.brotkrumen.storage.database;

import java.util.Locale;

public enum Engine {
    MYSQL,

    MARIADB,

    SQLITE;

    public static Engine getEngineByName(final String engine) {
        return switch (engine.toLowerCase(Locale.ROOT)) {
            case "mysql" -> MYSQL;
            case "mariadb" -> MARIADB;
            case "sqlite" -> SQLITE;
            default -> throw new IllegalArgumentException("Unknown database engine: " + engine);
        };
    }
}
