package com.spark.platform.services;

import com.spark.platform.config.DatabaseConfig;
import com.spark.platform.models.Sprint;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only service for Sprints (used by the Project Board sprint selector).
 */
public class SprintService {

    private final DatabaseConfig db = DatabaseConfig.getInstance();

    public List<Sprint> findByProject(int projectId) throws SQLException {
        String sql = "SELECT * FROM sprints WHERE project_id = ? ORDER BY sprint_number ASC";
        List<Sprint> sprints = new ArrayList<>();

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    sprints.add(mapRow(rs));
                }
            }
        }
        return sprints;
    }

    public Sprint findById(int sprintId) throws SQLException {
        String sql = "SELECT * FROM sprints WHERE sprint_id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sprintId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    private Sprint mapRow(ResultSet rs) throws SQLException {
        Sprint s = new Sprint();
        s.setSprintId(rs.getInt("sprint_id"));
        s.setProjectId(rs.getInt("project_id"));
        s.setSprintNumber(rs.getInt("sprint_number"));
        s.setTitle(rs.getString("title"));
        s.setStartDate(rs.getDate("start_date"));
        s.setEndDate(rs.getDate("end_date"));
        s.setGoal(rs.getString("goal"));
        s.setStatus(rs.getString("status"));
        s.setCreatedAt(rs.getTimestamp("created_at"));
        return s;
    }
}
