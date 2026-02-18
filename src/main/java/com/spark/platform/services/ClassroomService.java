package com.spark.platform.services;

import com.spark.platform.config.DatabaseConfig;
import com.spark.platform.models.Classroom;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClassroomService {

    private static final String DEFAULT_STATUS = "ACTIVE";
    private final DatabaseConfig databaseConfig;

    public ClassroomService() {
        this.databaseConfig = DatabaseConfig.getInstance();
    }

    public Classroom create(Classroom classroom) {
        String sql = "INSERT INTO classrooms (name, capacity, status) VALUES (?, ?, ?)";

        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, classroom.getName());
            statement.setInt(2, classroom.getCapacity());
            statement.setString(3, normalizeStatus(classroom.getStatus()));
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    classroom.setClassroomId(generatedKeys.getInt(1));
                }
            }
            return classroom;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create classroom", e);
        }
    }

    public Optional<Classroom> findById(int classroomId) {
        String sql = "SELECT classroom_id, name, capacity, status, created_at FROM classrooms WHERE classroom_id = ?";

        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, classroomId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRow(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch classroom by id", e);
        }
    }

    public List<Classroom> findAll() {
        String sql = "SELECT classroom_id, name, capacity, status, created_at FROM classrooms ORDER BY classroom_id";
        List<Classroom> classrooms = new ArrayList<>();

        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                classrooms.add(mapRow(resultSet));
            }
            return classrooms;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch classrooms", e);
        }
    }

    public boolean update(Classroom classroom) {
        String sql = "UPDATE classrooms SET name = ?, capacity = ?, status = ? WHERE classroom_id = ?";

        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, classroom.getName());
            statement.setInt(2, classroom.getCapacity());
            statement.setString(3, normalizeStatus(classroom.getStatus()));
            statement.setInt(4, classroom.getClassroomId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update classroom", e);
        }
    }

    public boolean delete(int classroomId) {
        String sql = "DELETE FROM classrooms WHERE classroom_id = ?";

        try (Connection connection = databaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, classroomId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete classroom", e);
        }
    }

    private Classroom mapRow(ResultSet resultSet) throws SQLException {
        Classroom classroom = new Classroom();
        classroom.setClassroomId(resultSet.getInt("classroom_id"));
        classroom.setName(resultSet.getString("name"));
        classroom.setCapacity(resultSet.getInt("capacity"));
        classroom.setStatus(resultSet.getString("status"));
        classroom.setCreatedAt(resultSet.getTimestamp("created_at"));
        return classroom;
    }

    private String normalizeStatus(String status) {
        return (status == null || status.isBlank()) ? DEFAULT_STATUS : status;
    }
}
