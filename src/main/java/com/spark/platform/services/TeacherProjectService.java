package com.spark.platform.services;

import com.spark.platform.config.DatabaseConfig;
import com.spark.platform.models.Classroom;
import com.spark.platform.models.Project;
import com.spark.platform.models.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service layer for teacher-side project management.
 * Handles: classroom listing, project CRUD per classroom,
 *          member assignment/removal, and summary statistics.
 */
public class TeacherProjectService {

    // ──── Classrooms ────

    /**
     * Get all active classrooms.
     * TODO: HARDCODED — returns ALL classrooms. Should filter by teacher's courses:
     *   SELECT DISTINCT c.* FROM classrooms c
     *   JOIN users u ON u.classroom_id = c.classroom_id
     *   JOIN student_courses sc ON sc.student_id = u.user_id
     *   JOIN teacher_courses tc ON tc.course_id = sc.course_id
     *   WHERE tc.teacher_id = ? AND c.status = 'ACTIVE'
     */
    public List<Classroom> findAllClassrooms() throws SQLException {
        List<Classroom> list = new ArrayList<>();
        String sql = "SELECT * FROM classrooms WHERE status = 'ACTIVE' ORDER BY name";
        try (Connection conn = db(); PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapClassroom(rs));
            }
        }
        return list;
    }

    /** Count students in a classroom. */
    public int countStudents(int classroomId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE classroom_id = ? AND user_type = 'STUDENT' AND status = 'ACTIVE'";
        try (Connection conn = db(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, classroomId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Count projects in a classroom.
     * TODO: HARDCODED — counts ALL projects in classroom. Should filter by teacher's courses:
     *   WHERE classroom_id = ? AND course_id IN (SELECT course_id FROM teacher_courses WHERE teacher_id = ?)
     */
    public int countProjectsInClassroom(int classroomId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM projects WHERE classroom_id = ?";
        try (Connection conn = db(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, classroomId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // ──── Projects per Classroom ────

    /**
     * Get all projects belonging to the given classroom.
     * TODO: HARDCODED — returns ALL projects in classroom. Should filter by teacher's courses:
     *   WHERE classroom_id = ? AND course_id IN (SELECT course_id FROM teacher_courses WHERE teacher_id = ?)
     */
    public List<Project> findProjectsByClassroom(int classroomId) throws SQLException {
        List<Project> list = new ArrayList<>();
        String sql = "SELECT * FROM projects WHERE classroom_id = ? ORDER BY title";
        try (Connection conn = db(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, classroomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapProject(rs));
                }
            }
        }
        return list;
    }

    /** Count members in a project. */
    public int countMembers(int projectId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM project_members WHERE project_id = ?";
        try (Connection conn = db(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // ──── Project Members ────

    /** Get members of a project with full user details and role. */
    public List<Map<String, Object>> findProjectMembers(int projectId) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT u.user_id, u.name, u.email, u.phone, u.skills_json, u.profile_image, " +
                     "pm.role_in_project FROM users u " +
                     "INNER JOIN project_members pm ON u.user_id = pm.user_id " +
                     "WHERE pm.project_id = ? ORDER BY pm.role_in_project, u.name";
        try (Connection conn = db(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("userId", rs.getInt("user_id"));
                    m.put("name", rs.getString("name"));
                    m.put("email", rs.getString("email"));
                    m.put("phone", rs.getString("phone"));
                    m.put("skillsJson", rs.getString("skills_json"));
                    m.put("profileImage", rs.getString("profile_image"));
                    m.put("role", rs.getString("role_in_project"));
                    list.add(m);
                }
            }
        }
        return list;
    }

    /** Get students from a classroom that are NOT already in a project. */
    public List<User> findAvailableStudents(int classroomId, int projectId) throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT u.user_id, u.name, u.email FROM users u " +
                     "WHERE u.classroom_id = ? AND u.user_type = 'STUDENT' AND u.status = 'ACTIVE' " +
                     "AND u.user_id NOT IN (SELECT pm.user_id FROM project_members pm WHERE pm.project_id = ?) " +
                     "ORDER BY u.name";
        try (Connection conn = db(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, classroomId);
            ps.setInt(2, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User u = new User();
                    u.setUserId(rs.getInt("user_id"));
                    u.setName(rs.getString("name"));
                    u.setEmail(rs.getString("email"));
                    list.add(u);
                }
            }
        }
        return list;
    }

    /** Add a member to a project. */
    public void addMember(int projectId, int userId, String role) throws SQLException {
        String sql = "INSERT INTO project_members (project_id, user_id, role_in_project) VALUES (?, ?, ?)";
        try (Connection conn = db(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            ps.setInt(2, userId);
            ps.setString(3, role != null ? role : "MEMBER");
            ps.executeUpdate();
        }
    }

    /** Remove a member from a project. */
    public void removeMember(int projectId, int userId) throws SQLException {
        String sql = "DELETE FROM project_members WHERE project_id = ? AND user_id = ?";
        try (Connection conn = db(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    // ──── Project CRUD ────

    /** Create a new project and return the generated ID. */
    public int createProject(Project project) throws SQLException {
        String sql = "INSERT INTO projects (title, description, repo_url, template_type, start_date, end_date, status, classroom_id, course_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = db();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, project.getTitle());
            ps.setString(2, project.getDescription());
            ps.setString(3, project.getRepoUrl());
            ps.setString(4, project.getTemplateType() != null ? project.getTemplateType() : "STANDARD");
            ps.setDate(5, project.getStartDate());
            ps.setDate(6, project.getEndDate());
            ps.setString(7, project.getStatus() != null ? project.getStatus() : "ACTIVE");
            if (project.getClassroomId() != null) {
                ps.setInt(8, project.getClassroomId());
            } else {
                ps.setNull(8, java.sql.Types.INTEGER);
            }
            if (project.getCourseId() != null) {
                ps.setInt(9, project.getCourseId());
            } else {
                ps.setNull(9, java.sql.Types.INTEGER);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    /** Delete a project (cascade deletes members via FK). */
    public void deleteProject(int projectId) throws SQLException {
        String sql = "DELETE FROM projects WHERE project_id = ?";
        try (Connection conn = db(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            ps.executeUpdate();
        }
    }

    // ──── Task Stats ────

    /** Get task completion stats for a project: {total, done, inProgress, todo, review}. */
    public Map<String, Integer> getTaskStats(int projectId) throws SQLException {
        Map<String, Integer> stats = new LinkedHashMap<>();
        String sql = "SELECT " +
                     "COUNT(*) as total, " +
                     "SUM(CASE WHEN status = 'DONE' THEN 1 ELSE 0 END) as done, " +
                     "SUM(CASE WHEN status = 'IN_PROGRESS' THEN 1 ELSE 0 END) as in_progress, " +
                     "SUM(CASE WHEN status = 'TODO' THEN 1 ELSE 0 END) as todo, " +
                     "SUM(CASE WHEN status = 'REVIEW' THEN 1 ELSE 0 END) as review " +
                     "FROM tasks WHERE project_id = ?";
        try (Connection conn = db(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.put("total", rs.getInt("total"));
                    stats.put("done", rs.getInt("done"));
                    stats.put("inProgress", rs.getInt("in_progress"));
                    stats.put("todo", rs.getInt("todo"));
                    stats.put("review", rs.getInt("review"));
                }
            }
        }
        return stats;
    }

    // ──── Helpers ────
    private Connection db() throws SQLException {
        return DatabaseConfig.getInstance().getConnection();
    }

    private Classroom mapClassroom(ResultSet rs) throws SQLException {
        Classroom c = new Classroom();
        c.setClassroomId(rs.getInt("classroom_id"));
        c.setName(rs.getString("name"));
        c.setCapacity(rs.getInt("capacity"));
        c.setStatus(rs.getString("status"));
        c.setCreatedAt(rs.getTimestamp("created_at"));
        return c;
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
