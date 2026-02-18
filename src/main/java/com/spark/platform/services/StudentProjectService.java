package com.spark.platform.services;

import com.spark.platform.config.DatabaseConfig;
import com.spark.platform.models.Project;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service layer for student-side project access.
 * Handles fetching projects a student is assigned to.
 */
public class StudentProjectService {

    /**
     * Get all projects where the given student is a member.
     * TODO: HARDCODED — studentId parameter will come from RBAC/session context
     */
    public List<Project> findProjectsByStudent(int studentId) throws SQLException {
        List<Project> list = new ArrayList<>();
        String sql = "SELECT p.* FROM projects p " +
                     "INNER JOIN project_members pm ON p.project_id = pm.project_id " +
                     "WHERE pm.user_id = ? " +
                     "ORDER BY p.title";
        try (Connection conn = db(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapProject(rs));
                }
            }
        }
        return list;
    }

    /**
     * Get a specific project by ID.
     */
    public Project findProjectById(int projectId) throws SQLException {
        String sql = "SELECT * FROM projects WHERE project_id = ?";
        try (Connection conn = db(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapProject(rs);
                }
            }
        }
        return null;
    }

    /**
     * Count members in a project.
     */
    public int countMembers(int projectId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM project_members WHERE project_id = ?";
        try (Connection conn = db(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // ──── Helpers ────
    private Connection db() throws SQLException {
        return DatabaseConfig.getInstance().getConnection();
    }

    private Project mapProject(ResultSet rs) throws SQLException {
        Project p = new Project();
        p.setProjectId(rs.getInt("project_id"));
        p.setTitle(rs.getString("title"));
        p.setDescription(rs.getString("description"));
        p.setRepoUrl(rs.getString("repo_url"));
        p.setBoardColumns(rs.getString("board_columns"));
        p.setTemplateType(rs.getString("template_type"));
        p.setStartDate(rs.getDate("start_date"));
        p.setEndDate(rs.getDate("end_date"));
        p.setStatus(rs.getString("status"));
        p.setClassroomId(rs.getObject("classroom_id", Integer.class));
        p.setCourseId(rs.getObject("course_id", Integer.class));
        p.setCreatedAt(rs.getTimestamp("created_at"));
        return p;
    }
}
