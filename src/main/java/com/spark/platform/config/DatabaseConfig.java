package com.spark.platform.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConfig {

    private static DatabaseConfig instance;
    private final String url;
    private final String username;
    private final String password;
    private final String driver;

    private DatabaseConfig() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("db.properties")) {
            if (input == null) {
                throw new RuntimeException(
                    "db.properties not found!\n" +
                    "Copy db.properties.example to db.properties and fill in your credentials."
                );
            }
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load db.properties", e);
        }

        this.url = props.getProperty("db.url");
        this.username = props.getProperty("db.username");
        this.password = props.getProperty("db.password");
        this.driver = props.getProperty("db.driver", "com.mysql.cj.jdbc.Driver");

        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver not found. Add mysql-connector-j to pom.xml", e);
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
        return DriverManager.getConnection(url, username, password);
    }

    /**
     * Test the connection and print status.
     * Call this from MainApp on startup to verify DB is reachable.
     */
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            System.out.println("✅ Database connected: " + conn.getCatalog());
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Database connection failed: " + e.getMessage());
            return false;
        }
    }
}