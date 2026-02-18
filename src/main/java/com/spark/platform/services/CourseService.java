package com.spark.platform.services;

import com.spark.platform.config.DatabaseConfig;
import com.spark.platform.models.Course;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CourseService {

    private static final String DEFAULT_STATUS = "ACTIVE";
    private final DatabaseConfig databaseConfig;

    public CourseService() {
        this.databaseConfig = DatabaseConfig.getInstance();
    }

    public Course create(Course course) {
        String sql = "INSERT INTO courses " +
                "(title, code, total_hours, hours_completed, coefficient, cc_weight, tp_weight, exam_weight, semester, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bindCourseParameters(statement, course);
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    course.setCourseId(generatedKeys.getInt(1));
                }
            }
            return course;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create course", e);
        }
    }

    public Optional<Course> findById(int courseId) {
        String sql = "SELECT course_id, title, code, total_hours, hours_completed, coefficient, cc_weight, tp_weight, exam_weight, semester, status, created_at " +
                "FROM courses WHERE course_id = ?";

        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, courseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch course by id", e);
        }
    }

    public List<Course> findAll() {
        String sql = "SELECT course_id, title, code, total_hours, hours_completed, coefficient, cc_weight, tp_weight, exam_weight, semester, status, created_at " +
                "FROM courses ORDER BY course_id";
        List<Course> courses = new ArrayList<>();

        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                courses.add(mapRow(resultSet));
            }
            return courses;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch courses", e);
        }
    }

    public boolean update(Course course) {
        String sql = "UPDATE courses SET " +
                "title = ?, code = ?, total_hours = ?, hours_completed = ?, coefficient = ?, cc_weight = ?, tp_weight = ?, exam_weight = ?, semester = ?, status = ? " +
                "WHERE course_id = ?";

        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            bindCourseParameters(statement, course);
            statement.setInt(11, course.getCourseId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update course", e);
        }
    }

    public boolean delete(int courseId) {
        String sql = "DELETE FROM courses WHERE course_id = ?";

        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, courseId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete course", e);
        }
    }

    private void bindCourseParameters(PreparedStatement statement, Course course) throws SQLException {
        statement.setString(1, course.getTitle());
        statement.setString(2, course.getCode());
        statement.setFloat(3, course.getTotalHours());
        statement.setFloat(4, course.getHoursCompleted());
        statement.setFloat(5, course.getCoefficient());
        statement.setFloat(6, course.getCcWeight());
        statement.setFloat(7, course.getTpWeight());
        statement.setFloat(8, course.getExamWeight());

        if (course.getSemester() == null) {
            statement.setNull(9, java.sql.Types.INTEGER);
        } else {
            statement.setInt(9, course.getSemester());
        }

        statement.setString(10, normalizeStatus(course.getStatus()));
    }

    private Course mapRow(ResultSet resultSet) throws SQLException {
        Course course = new Course();
        course.setCourseId(resultSet.getInt("course_id"));
        course.setTitle(resultSet.getString("title"));
        course.setCode(resultSet.getString("code"));
        course.setTotalHours(resultSet.getFloat("total_hours"));
        course.setHoursCompleted(resultSet.getFloat("hours_completed"));
        course.setCoefficient(resultSet.getFloat("coefficient"));
        course.setCcWeight(resultSet.getFloat("cc_weight"));
        course.setTpWeight(resultSet.getFloat("tp_weight"));
        course.setExamWeight(resultSet.getFloat("exam_weight"));

        int semester = resultSet.getInt("semester");
        course.setSemester(resultSet.wasNull() ? null : semester);

        course.setStatus(resultSet.getString("status"));
        course.setCreatedAt(resultSet.getTimestamp("created_at"));
        return course;
    }

    private String normalizeStatus(String status) {
        return (status == null || status.isBlank()) ? DEFAULT_STATUS : status;
    }
}
