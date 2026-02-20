package com.spark.platform.config;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Abstraction over a database connection source.
 * Implementations: MySQLConnectionProvider, H2ConnectionProvider.
 */
public interface DatabaseConnectionProvider {
    Connection getConnection() throws SQLException;
    boolean testConnection();
    String getProviderName();
}
