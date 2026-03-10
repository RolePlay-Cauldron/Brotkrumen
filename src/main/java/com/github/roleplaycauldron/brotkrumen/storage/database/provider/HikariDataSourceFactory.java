package com.github.roleplaycauldron.brotkrumen.storage.database.provider;

import com.github.roleplaycauldron.brotkrumen.storage.StorageException;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Factory class for creating and configuring HikariDataSource instances.
 * <p>
 * This class provides a method to construct a fully configured
 * {@link HikariDataSource} object based on the given
 * configuration parameters.
 * <p>
 * The factory uses the HikariCP connection pooling library, which is
 * designed to optimize JDBC connection pool performance and management.
 */
public final class HikariDataSourceFactory {

    private HikariDataSourceFactory() {
    }

    /**
     * Creates and configures a {@link HikariDataSource} instance using the provided
     * configuration section, JDBC URL, and pool name.
     *
     * @param configSection the configuration section containing necessary properties such as
     *                      user credentials, pool size, and timing configurations
     * @param startJdbcUrl  the JDBC URL to connect to the database
     * @return a fully configured {@link HikariDataSource} instance
     */
    /* default */
    static HikariDataSource create(final ConfigurationSection configSection,
                                   final String startJdbcUrl) {
        final HikariConfig databaseConfig = new HikariConfig();

        final String jdbcUrl = startJdbcUrl + configSection.getString("host") + ":"
                + configSection.getString("port") + "/"
                + configSection.getString("database");
        databaseConfig.setJdbcUrl(jdbcUrl);

        final String username = configSection.getString("user");
        if (username != null) {
            databaseConfig.setUsername(username);
        }
        final String password = configSection.getString("password");
        if (password != null) {
            databaseConfig.setPassword(password);
        }

        databaseConfig.setPoolName("Brotkrumen-ConnectionPool");

        final ConfigurationSection poolConfig = configSection.getConfigurationSection("poolSettings");
        if (poolConfig == null) {
            throw new StorageException("Database pool configuration is missing");
        }

        final int maximumPoolSize = poolConfig.getInt("maximumPoolSize");
        databaseConfig.setMaximumPoolSize(maximumPoolSize);

        final int minimumIdle = poolConfig.getInt("minimumIdle");
        databaseConfig.setMinimumIdle(minimumIdle);

        final int maxLifetime = poolConfig.getInt("maximumLifetime");
        databaseConfig.setMaxLifetime(maxLifetime);

        final int keepAliveTime = poolConfig.getInt("keepAliveTime");
        databaseConfig.setKeepaliveTime(keepAliveTime);

        final int connectionTimeout = poolConfig.getInt("connectionTimeout");
        databaseConfig.setConnectionTimeout(connectionTimeout);

        final ThreadFactoryBuilder builder = new ThreadFactoryBuilder();
        builder.setNameFormat("HikariThread-%d");
        databaseConfig.setThreadFactory(builder.build());

        return new HikariDataSource(databaseConfig);
    }
}
