package com.spark.platform.services;

import com.spark.platform.config.DatabaseConfig;
import com.spark.platform.models.Task;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * CRUD service for Tasks.
 * Uses the DAO pattern with the shared DatabaseConfig singleton.
 */
public class TaskService {

    private final DatabaseConfig db = DatabaseConfig.getInstance();

    // ──── CREATE ────
    public Task create(Task task) throws SQLException {
        String sql = "INSERT INTO tasks (project_id, sprint_id, title, description, assigned_to, " +
                     "column_name, priority, estimated_hours, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, task.getProjectId());
            setNullableInt(ps, 2, task.getSprintId());
            ps.setString(3, task.getTitle());
            ps.setString(4, task.getDescription());
            setNullableInt(ps, 5, task.getAssignedTo());
            ps.setString(6, task.getColumnName());
            ps.setString(7, task.getPriority());
            setNullableFloat(ps, 8, task.getEstimatedHours());
            ps.setString(9, task.getStatus());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    task.setTaskId(keys.getInt(1));
                }
            }
        }
        return task;
    }

    // ──── READ ALL (by project + optional sprint) ────
    public List<Task> findByProjectAndSprint(int projectId, Integer sprintId) throws SQLException {
        String sql;
        if (sprintId != null) {
            sql = "SELECT * FROM tasks WHERE project_id = ? AND sprint_id = ? ORDER BY created_at DESC";
        } else {
            sql = "SELECT * FROM tasks WHERE project_id = ? ORDER BY created_at DESC";
        }

        List<Task> tasks = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, projectId);
            if (sprintId != null) {
                ps.setInt(2, sprintId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tasks.add(mapRow(rs));
                }
            }
        }
        return tasks;
    }

    // ──── READ ONE ────
    public Task findById(int taskId) throws SQLException {
        String sql = "SELECT * FROM tasks WHERE task_id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    // ──── UPDATE ────
    public void update(Task task) throws SQLException {
        String sql = "UPDATE tasks SET sprint_id = ?, title = ?, description = ?, assigned_to = ?, " +
                     "column_name = ?, priority = ?, estimated_hours = ?, status = ? " +
                     "WHERE task_id = ?";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            setNullableInt(ps, 1, task.getSprintId());
            ps.setString(2, task.getTitle());
            ps.setString(3, task.getDescription());
            setNullableInt(ps, 4, task.getAssignedTo());
            ps.setString(5, task.getColumnName());
            ps.setString(6, task.getPriority());
            setNullableFloat(ps, 7, task.getEstimatedHours());
            ps.setString(8, task.getStatus());
            ps.setInt(9, task.getTaskId());

            ps.executeUpdate();
        }
    }

    // ──── UPDATE STATUS / COLUMN (for drag-and-drop style moves) ────
    public void updateStatus(int taskId, String status, String columnName) throws SQLException {
        String sql = "UPDATE tasks SET status = ?, column_name = ? WHERE task_id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, columnName);
            ps.setInt(3, taskId);
            ps.executeUpdate();
        }
    }

    // ──── DELETE ────
    public void delete(int taskId) throws SQLException {
        String sql = "DELETE FROM tasks WHERE task_id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            ps.executeUpdate();
        }
    }

    // ──── ROW MAPPER ────
    private Task mapRow(ResultSet rs) throws SQLException {
        Task t = new Task();
        t.setTaskId(rs.getInt("task_id"));
        t.setProjectId(rs.getInt("project_id"));

        int sprintId = rs.getInt("sprint_id");
        t.setSprintId(rs.wasNull() ? null : sprintId);

        t.setTitle(rs.getString("title"));
        t.setDescription(rs.getString("description"));

        int assignedTo = rs.getInt("assigned_to");
        t.setAssignedTo(rs.wasNull() ? null : assignedTo);

        t.setColumnName(rs.getString("column_name"));
        t.setPriority(rs.getString("priority"));

        float hours = rs.getFloat("estimated_hours");
        t.setEstimatedHours(rs.wasNull() ? null : hours);

        t.setStatus(rs.getString("status"));
        t.setCreatedAt(rs.getTimestamp("created_at"));
        t.setUpdatedAt(rs.getTimestamp("updated_at"));
        return t;
    }

    // ──── HELPERS ────
    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) ps.setNull(index, Types.INTEGER);
        else ps.setInt(index, value);
    }

    private void setNullableFloat(PreparedStatement ps, int index, Float value) throws SQLException {
        if (value == null) ps.setNull(index, Types.FLOAT);
        else ps.setFloat(index, value);
    }
}
