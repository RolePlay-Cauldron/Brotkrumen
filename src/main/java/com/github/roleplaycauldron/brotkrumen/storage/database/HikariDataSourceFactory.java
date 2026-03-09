package com.github.roleplaycauldron.brotkrumen.storage.database;

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
public class HikariDataSourceFactory {

    private HikariDataSourceFactory() {
    }

    /**
     * Creates and configures a {@link HikariDataSource} instance using the provided
     * configuration section, JDBC URL, and pool name.
     *
     * @param configSection the configuration section containing necessary properties such as
     *                      user credentials, pool size, and timing configurations
     * @param jdbcUrl       the JDBC URL to connect to the database
     * @param poolName      the name of the connection pool to be created
     * @return a fully configured {@link HikariDataSource} instance
     */
    /* default */
    static HikariDataSource create(final ConfigurationSection configSection,
                                   final String jdbcUrl,
                                   final String poolName) {
        final HikariConfig databaseConfig = new HikariConfig();

        databaseConfig.setJdbcUrl(jdbcUrl);

        final String username = configSection.getString("user");
        if (username != null) {
            databaseConfig.setUsername(username);
        }
        final String password = configSection.getString("password");
        if (password != null) {
            databaseConfig.setPassword(password);
        }

        databaseConfig.setPoolName(poolName);

        final int maximumPoolSize = configSection.getInt("maximumPoolSize");
        databaseConfig.setMaximumPoolSize(maximumPoolSize);

        final int minimumIdle = configSection.getInt("minimumIdle");
        databaseConfig.setMinimumIdle(minimumIdle);

        final int maxLifetime = configSection.getInt("maximumLifetime");
        databaseConfig.setMaxLifetime(maxLifetime);

        final int keepAliveTime = configSection.getInt("keepAliveTime");
        databaseConfig.setKeepaliveTime(keepAliveTime);

        final int connectionTimeout = configSection.getInt("connectionTimeout");
        databaseConfig.setConnectionTimeout(connectionTimeout);

        final ThreadFactoryBuilder builder = new ThreadFactoryBuilder();
        builder.setNameFormat("HikariThread-%d");
        databaseConfig.setThreadFactory(builder.build());

        return new HikariDataSource(databaseConfig);
    }
}
