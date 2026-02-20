package com.spark.platform.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class MySQLConnectionProvider implements DatabaseConnectionProvider {

    private final String url;
    private final String username;
    private final String password;

    public MySQLConnectionProvider(Properties props) {
        this.url = props.getProperty("db.url");
        this.username = props.getProperty("db.username");
        this.password = props.getProperty("db.password");
        String driver = props.getProperty("db.driver", "com.mysql.cj.jdbc.Driver");
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC driver not found", e);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    @Override
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            System.out.println("[DB] MySQL connected: " + conn.getCatalog());
            return true;
        } catch (SQLException e) {
            System.err.println("[DB] MySQL connection failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "MySQL";
    }
}
