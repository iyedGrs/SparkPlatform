package com.spark.platform.services;

import com.spark.platform.config.DatabaseConfig;
import com.spark.platform.models.User;
import com.spark.platform.utils.PasswordUtils;

import java.security.SecureRandom;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService {

    private final DatabaseConfig db = DatabaseConfig.getInstance();

    // ──── AUTHENTICATION ────

    /**
     * Authenticate a user by email and BCrypt password.
     * Returns the User if valid, null if not found or password mismatch.
     */
    public User authenticate(String email, String plainPassword) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ? AND status = 'ACTIVE'";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = mapRow(rs);
                    if (PasswordUtils.verify(plainPassword, user.getPassword())) {
                        return user;
                    }
                }
            }
        }
        return null;
    }

    // ──── CREATE ────

    /**
     * Creates a user with a hashed password.
     * The caller passes the plain-text password; this method hashes it.
     * Returns the User with the generated userId set.
     */
    public User create(User user, String plainPassword) throws SQLException {
        String hash = PasswordUtils.hash(plainPassword);
        String sql = "INSERT INTO users (name, email, password, user_type, classroom_id, " +
                "phone, skills_json, profile_image, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, hash);
            ps.setString(4, user.getUserType());
            if (user.getClassroomId() != null) {
                ps.setInt(5, user.getClassroomId());
            } else {
                ps.setNull(5, Types.INTEGER);
            }
            ps.setString(6, user.getPhone());
            ps.setString(7, user.getSkillsJson());
            ps.setString(8, user.getProfileImage());
            ps.setString(9, user.getStatus() != null ? user.getStatus() : "ACTIVE");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setUserId(keys.getInt(1));
                }
            }
        }
        user.setPassword(hash);
        return user;
    }

    // ──── READ ────

    public User findById(int userId) throws SQLException {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public List<User> findAll() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY user_type, name";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(mapRow(rs));
            }
        }
        return users;
    }

    public List<User> findByType(String userType) throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE user_type = ? AND status = 'ACTIVE' ORDER BY name";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userType);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(mapRow(rs));
                }
            }
        }
        return users;
    }

    // ──── UPDATE ────

    public void update(User user) throws SQLException {
        String sql = "UPDATE users SET name = ?, email = ?, user_type = ?, classroom_id = ?, " +
                "phone = ?, skills_json = ?, profile_image = ?, status = ? " +
                "WHERE user_id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getUserType());
            if (user.getClassroomId() != null) {
                ps.setInt(4, user.getClassroomId());
            } else {
                ps.setNull(4, Types.INTEGER);
            }
            ps.setString(5, user.getPhone());
            ps.setString(6, user.getSkillsJson());
            ps.setString(7, user.getProfileImage());
            ps.setString(8, user.getStatus());
            ps.setInt(9, user.getUserId());
            ps.executeUpdate();
        }
    }

    public void updatePassword(int userId, String newPlainPassword) throws SQLException {
        String hash = PasswordUtils.hash(newPlainPassword);
        String sql = "UPDATE users SET password = ? WHERE user_id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    // ──── DELETE ────

    public void delete(int userId) throws SQLException {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    // ──── SEED ────

    /**
     * Ensures a default admin account exists.
     * Called on startup by MainApp. Idempotent.
     */
    public void ensureAdminExists() throws SQLException {
        User existing = findByEmail("admin@spark.tn");
        if (existing == null) {
            User admin = new User();
            admin.setName("Admin Principal");
            admin.setEmail("admin@spark.tn");
            admin.setUserType("ADMINISTRATOR");
            admin.setPhone("+21612345678");
            admin.setStatus("ACTIVE");
            create(admin, "admin123");
            System.out.println("[Auth] Default admin account created (admin@spark.tn / admin123)");
        }
    }

    // ──── PASSWORD GENERATION ────

    /**
     * Generates a random 10-character password.
     * Excludes ambiguous characters (0, O, l, 1, I).
     */
    public static String generatePassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // ──── CLASSROOMS HELPER ────

    /**
     * Loads all classrooms for use in ComboBoxes.
     * Returns list of {classroomId, name} pairs.
     */
    public List<int[]> findAllClassroomIds() throws SQLException {
        List<int[]> ids = new ArrayList<>();
        String sql = "SELECT classroom_id FROM classrooms WHERE status = 'ACTIVE' ORDER BY name";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ids.add(new int[]{rs.getInt("classroom_id")});
            }
        }
        return ids;
    }

    /**
     * Loads classroom name by id.
     */
    public String findClassroomName(int classroomId) throws SQLException {
        String sql = "SELECT name FROM classrooms WHERE classroom_id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, classroomId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("name");
            }
        }
        return null;
    }

    /**
     * Loads all classrooms as id -> name map entries.
     */
    public List<Object[]> findAllClassrooms() throws SQLException {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT classroom_id, name FROM classrooms WHERE status = 'ACTIVE' ORDER BY name";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Object[]{rs.getInt("classroom_id"), rs.getString("name")});
            }
        }
        return list;
    }

    // ──── ROW MAPPER ────

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setName(rs.getString("name"));
        u.setEmail(rs.getString("email"));
        u.setPassword(rs.getString("password"));
        u.setUserType(rs.getString("user_type"));
        int classroomId = rs.getInt("classroom_id");
        u.setClassroomId(rs.wasNull() ? null : classroomId);
        u.setPhone(rs.getString("phone"));
        u.setSkillsJson(rs.getString("skills_json"));
        u.setProfileImage(rs.getString("profile_image"));
        u.setStatus(rs.getString("status"));
        u.setCreatedAt(rs.getTimestamp("created_at"));
        u.setUpdatedAt(rs.getTimestamp("updated_at"));
        return u;
    }
}
