package com.spark.platform.services;

import com.spark.platform.config.DatabaseConfig;
import com.spark.platform.models.Session;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SessionService {

    private static final String DEFAULT_STATUS = "SCHEDULED";
    private final DatabaseConfig databaseConfig;

    public SessionService() {
        this.databaseConfig = DatabaseConfig.getInstance();
    }

    public Session create(Session session) {
        String sql = "INSERT INTO sessions " +
                "(course_id, classroom_id, teacher_id, day_of_week, start_time, end_time, duration_hours, session_date, recurring, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bindSessionParameters(statement, session);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    session.setSessionId(generatedKeys.getInt(1));
                }
            }
            return session;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create session", e);
        }
    }

    public Optional<Session> findById(int sessionId) {
        String sql = "SELECT session_id, course_id, classroom_id, teacher_id, day_of_week, start_time, end_time, duration_hours, session_date, recurring, status, created_at " +
                "FROM sessions WHERE session_id = ?";

        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, sessionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch session by id", e);
        }
    }

    public List<Session> findAll() {
        String sql = "SELECT session_id, course_id, classroom_id, teacher_id, day_of_week, start_time, end_time, duration_hours, session_date, recurring, status, created_at " +
                "FROM sessions ORDER BY session_id";
        List<Session> sessions = new ArrayList<>();

        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                sessions.add(mapRow(resultSet));
            }
            return sessions;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch sessions", e);
        }
    }

    public boolean update(Session session) {
        String sql = "UPDATE sessions SET " +
                "course_id = ?, classroom_id = ?, teacher_id = ?, day_of_week = ?, start_time = ?, end_time = ?, duration_hours = ?, session_date = ?, recurring = ?, status = ? " +
                "WHERE session_id = ?";

        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            bindSessionParameters(statement, session);
            statement.setInt(11, session.getSessionId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update session", e);
        }
    }

    public boolean delete(int sessionId) {
        String sql = "DELETE FROM sessions WHERE session_id = ?";

        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, sessionId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete session", e);
        }
    }

    private void bindSessionParameters(PreparedStatement statement, Session session) throws SQLException {
        statement.setInt(1, session.getCourseId());
        setNullableInt(statement, 2, session.getClassroomId());
        setNullableInt(statement, 3, session.getTeacherId());
        statement.setString(4, session.getDayOfWeek());
        statement.setTime(5, session.getStartTime());
        statement.setTime(6, session.getEndTime());
        statement.setFloat(7, session.getDurationHours());
        statement.setDate(8, session.getSessionDate());
        statement.setBoolean(9, session.isRecurring());
        statement.setString(10, normalizeStatus(session.getStatus()));
    }

    private Session mapRow(ResultSet resultSet) throws SQLException {
        Session session = new Session();
        session.setSessionId(resultSet.getInt("session_id"));
        session.setCourseId(resultSet.getInt("course_id"));

        int classroomId = resultSet.getInt("classroom_id");
        session.setClassroomId(resultSet.wasNull() ? null : classroomId);

        int teacherId = resultSet.getInt("teacher_id");
        session.setTeacherId(resultSet.wasNull() ? null : teacherId);

        session.setDayOfWeek(resultSet.getString("day_of_week"));
        session.setStartTime(resultSet.getTime("start_time"));
        session.setEndTime(resultSet.getTime("end_time"));
        session.setDurationHours(resultSet.getFloat("duration_hours"));
        session.setSessionDate(resultSet.getDate("session_date"));
        session.setRecurring(resultSet.getBoolean("recurring"));
        session.setStatus(resultSet.getString("status"));
        session.setCreatedAt(resultSet.getTimestamp("created_at"));
        return session;
    }

    private void setNullableInt(PreparedStatement statement, int parameterIndex, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(parameterIndex, java.sql.Types.INTEGER);
        } else {
            statement.setInt(parameterIndex, value);
        }
    }

    private String normalizeStatus(String status) {
        return (status == null || status.isBlank()) ? DEFAULT_STATUS : status;
    }
}
