package com.spark.platform.config;

import com.spark.platform.utils.PasswordUtils;

import java.sql.*;

/**
 * H2 in-memory database provider.
 * Used as fallback when MySQL is unavailable.
 * Auto-creates schema and seeds default data on first use.
 */
public class H2ConnectionProvider implements DatabaseConnectionProvider {

    private static final String H2_URL =
            "jdbc:h2:mem:sparkplatform;DB_CLOSE_DELAY=-1;MODE=MySQL";
    private boolean initialized = false;

    public H2ConnectionProvider() {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("H2 driver not found", e);
        }
        initializeSchema();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(H2_URL, "sa", "");
    }

    @Override
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            System.out.println("[DB] H2 in-memory database active");
            return true;
        } catch (SQLException e) {
            System.err.println("[DB] H2 connection failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "H2 (in-memory)";
    }

    private synchronized void initializeSchema() {
        if (initialized) return;
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(CLASSROOMS_DDL);
            stmt.execute(USERS_DDL);
            stmt.execute(USERS_EMAIL_IDX);
            stmt.execute(USERS_TYPE_IDX);
            stmt.execute(NOTIFICATIONS_DDL);
            stmt.execute(NOTIFICATIONS_USER_IDX);
            seedData(conn);
            initialized = true;
            System.out.println("[DB] H2 schema initialized with seed data");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize H2 schema", e);
        }
    }

    private void seedData(Connection conn) throws SQLException {
        // Seed classrooms
        String insertClassroom = "INSERT INTO classrooms (name, capacity, status) VALUES (?, ?, 'ACTIVE')";
        try (PreparedStatement ps = conn.prepareStatement(insertClassroom)) {
            String[][] classrooms = {{"GL-A", "35"}, {"GL-B", "35"}, {"DS-A", "30"}, {"IOT-A", "30"}};
            for (String[] cr : classrooms) {
                ps.setString(1, cr[0]);
                ps.setInt(2, Integer.parseInt(cr[1]));
                ps.addBatch();
            }
            ps.executeBatch();
        }

        // Seed admin account
        String hash = PasswordUtils.hash("admin123");
        String insertUser = "INSERT INTO users (name, email, password, user_type, phone, status) " +
                "VALUES (?, ?, ?, ?, ?, 'ACTIVE')";
        try (PreparedStatement ps = conn.prepareStatement(insertUser)) {
            ps.setString(1, "Admin Principal");
            ps.setString(2, "admin@spark.tn");
            ps.setString(3, hash);
            ps.setString(4, "ADMINISTRATOR");
            ps.setString(5, "+21612345678");
            ps.executeUpdate();
        }
    }

    // ── H2-compatible DDL ──

    private static final String CLASSROOMS_DDL =
            "CREATE TABLE IF NOT EXISTS classrooms (" +
                    "classroom_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "capacity INT DEFAULT 30, " +
                    "status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")";

    private static final String USERS_DDL =
            "CREATE TABLE IF NOT EXISTS users (" +
                    "user_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "email VARCHAR(150) NOT NULL, " +
                    "password VARCHAR(255) NOT NULL, " +
                    "user_type VARCHAR(20) NOT NULL CHECK (user_type IN ('STUDENT','TEACHER','ADMINISTRATOR')), " +
                    "classroom_id INT DEFAULT NULL, " +
                    "phone VARCHAR(20) DEFAULT NULL, " +
                    "skills_json CLOB DEFAULT NULL, " +
                    "profile_image VARCHAR(500) DEFAULT NULL, " +
                    "status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "CONSTRAINT uq_users_email UNIQUE (email), " +
                    "CONSTRAINT fk_users_classroom FOREIGN KEY (classroom_id) REFERENCES classrooms(classroom_id) ON DELETE SET NULL" +
                    ")";

    private static final String USERS_EMAIL_IDX =
            "CREATE INDEX IF NOT EXISTS idx_users_email ON users(email)";

    private static final String USERS_TYPE_IDX =
            "CREATE INDEX IF NOT EXISTS idx_users_type ON users(user_type)";

    private static final String NOTIFICATIONS_DDL =
            "CREATE TABLE IF NOT EXISTS notifications (" +
                    "notification_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "user_id INT NOT NULL, " +
                    "title VARCHAR(200) NOT NULL, " +
                    "message CLOB, " +
                    "type VARCHAR(20) DEFAULT 'IN_APP' CHECK (type IN ('SMS','IN_APP','EMAIL')), " +
                    "related_type VARCHAR(50) DEFAULT NULL, " +
                    "related_id INT DEFAULT NULL, " +
                    "is_read BOOLEAN DEFAULT FALSE, " +
                    "status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE', " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE" +
                    ")";

    private static final String NOTIFICATIONS_USER_IDX =
            "CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id)";
}
