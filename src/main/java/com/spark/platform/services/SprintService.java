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

    // ──── CREATE ────
    public Sprint create(Sprint sprint) throws SQLException {
        String sql = "INSERT INTO sprints (project_id, sprint_number, title, start_date, end_date, goal, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, sprint.getProjectId());
            ps.setInt(2, sprint.getSprintNumber());
            ps.setString(3, sprint.getTitle());
            ps.setDate(4, sprint.getStartDate());
            ps.setDate(5, sprint.getEndDate());
            ps.setString(6, sprint.getGoal());
            ps.setString(7, sprint.getStatus() != null ? sprint.getStatus() : "PLANNED");
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    sprint.setSprintId(keys.getInt(1));
                }
            }
        }
        return sprint;
    }

    /** Returns the next sprint number for a project. */
    public int getNextSprintNumber(int projectId) throws SQLException {
        String sql = "SELECT COALESCE(MAX(sprint_number), 0) + 1 FROM sprints WHERE project_id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 1;
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
