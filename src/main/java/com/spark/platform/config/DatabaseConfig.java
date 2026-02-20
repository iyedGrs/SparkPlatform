package com.spark.platform.config;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Database connection factory.
 * Tries MySQL first (from db.properties). Falls back to H2 in-memory.
 * Singleton: all services use DatabaseConfig.getInstance().getConnection().
 */
public class DatabaseConfig {

    private static DatabaseConfig instance;
    private final DatabaseConnectionProvider provider;

    private DatabaseConfig() {
        DatabaseConnectionProvider resolved = null;

        // 1. Try MySQL
        try {
            Properties props = loadProperties();
            if (props != null) {
                MySQLConnectionProvider mysql = new MySQLConnectionProvider(props);
                if (mysql.testConnection()) {
                    resolved = mysql;
                }
            }
        } catch (Exception e) {
            System.err.println("[DB] MySQL unavailable: " + e.getMessage());
        }

        // 2. Fall back to H2
        if (resolved == null) {
            System.out.println("[DB] Falling back to H2 in-memory database...");
            resolved = new H2ConnectionProvider();
        }

        this.provider = resolved;
        System.out.println("[DB] Using: " + provider.getProviderName());
    }

    private Properties loadProperties() {
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (input == null) return null;
            Properties props = new Properties();
            props.load(input);
            return props;
        } catch (Exception e) {
            return null;
        }
    }

    public static DatabaseConfig getInstance() {
        if (instance == null) {
            synchronized (DatabaseConfig.class) {
                if (instance == null) {
                    instance = new DatabaseConfig();
                }
            }
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return provider.getConnection();
    }

    public boolean testConnection() {
        return provider.testConnection();
    }

    public String getProviderName() {
        return provider.getProviderName();
    }
}
