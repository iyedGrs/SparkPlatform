package com.classroom.services;

import com.spark.platform.config.DatabaseConfig;
import com.spark.platform.models.Material;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcMaterialRepository implements MaterialRepository {

    private static final String SELECT_ALL = """
            SELECT material_id, type, title, file_path, content, visibility, status, created_at
            FROM materials
            ORDER BY material_id
            """;

    private static final String SELECT_BY_ID = """
            SELECT material_id, type, title, file_path, content, visibility, status, created_at
            FROM materials
            WHERE material_id = ?
            """;

    private static final String INSERT_SQL = """
            INSERT INTO materials (type, title, file_path, content, visibility, status)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_SQL = """
            UPDATE materials
            SET type = ?, title = ?, file_path = ?, content = ?, visibility = ?, status = ?
            WHERE material_id = ?
            """;

    private static final String ARCHIVE_SQL = """
            UPDATE materials
            SET status = 'ARCHIVED'
            WHERE material_id = ?
            """;

    private final DatabaseConfig databaseConfig;

    public JdbcMaterialRepository(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    public boolean isAvailable() {
        try (Connection ignored = databaseConfig.getConnection()) {
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public List<Material> findAll() {
        List<Material> materials = new ArrayList<>();
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ALL);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                materials.add(mapRow(rs));
            }
            return materials;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list materials", e);
        }
    }

    @Override
    public Optional<Material> findById(int materialId) {
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID)) {
            stmt.setInt(1, materialId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find material by id " + materialId, e);
        }
    }

    @Override
    public Material create(Material material) {
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, material.getType());
            stmt.setString(2, material.getTitle());
            stmt.setString(3, material.getFilePath());
            stmt.setString(4, material.getContent());
            stmt.setString(5, material.getVisibility());
            stmt.setString(6, material.getStatus());
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new RuntimeException("Material created but no generated ID returned");
                }
                int id = generatedKeys.getInt(1);
                return findById(id).orElseThrow(() -> new RuntimeException("Material created but not found"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create material", e);
        }
    }

    @Override
    public Material update(Material material) {
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {

            stmt.setString(1, material.getType());
            stmt.setString(2, material.getTitle());
            stmt.setString(3, material.getFilePath());
            stmt.setString(4, material.getContent());
            stmt.setString(5, material.getVisibility());
            stmt.setString(6, material.getStatus());
            stmt.setInt(7, material.getMaterialId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new IllegalArgumentException("Material not found: " + material.getMaterialId());
            }
            return findById(material.getMaterialId())
                    .orElseThrow(() -> new RuntimeException("Updated material not found"));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update material", e);
        }
    }

    @Override
    public void archive(int materialId) {
        try (Connection conn = databaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(ARCHIVE_SQL)) {
            stmt.setInt(1, materialId);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new IllegalArgumentException("Material not found: " + materialId);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to archive material", e);
        }
    }

    @Override
    public String backendName() {
        return "MySQL";
    }

    private Material mapRow(ResultSet rs) throws SQLException {
        Material material = new Material();
        material.setMaterialId(rs.getInt("material_id"));
        material.setType(rs.getString("type"));
        material.setTitle(rs.getString("title"));
        material.setFilePath(rs.getString("file_path"));
        material.setContent(rs.getString("content"));
        material.setVisibility(rs.getString("visibility"));
        material.setStatus(rs.getString("status"));
        material.setCreatedAt(rs.getTimestamp("created_at"));
        return material;
    }
}
