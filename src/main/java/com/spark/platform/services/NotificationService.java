package com.spark.platform.services;

import com.spark.platform.config.DatabaseConfig;
import com.spark.platform.models.Notification;

import java.sql.*;

public class NotificationService {

    private final DatabaseConfig db = DatabaseConfig.getInstance();

    public Notification create(Notification notification) throws SQLException {
        String sql = "INSERT INTO notifications (user_id, title, message, type, " +
                "related_type, related_id, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, notification.getUserId());
            ps.setString(2, notification.getTitle());
            ps.setString(3, notification.getMessage());
            ps.setString(4, notification.getType() != null ? notification.getType() : "IN_APP");
            ps.setString(5, notification.getRelatedType());
            if (notification.getRelatedId() != null) {
                ps.setInt(6, notification.getRelatedId());
            } else {
                ps.setNull(6, Types.INTEGER);
            }
            ps.setString(7, notification.getStatus() != null ? notification.getStatus() : "ACTIVE");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    notification.setNotificationId(keys.getInt(1));
                }
            }
        }
        return notification;
    }
}
