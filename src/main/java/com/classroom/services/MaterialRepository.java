package com.classroom.services;

import com.spark.platform.models.Material;

import java.util.List;
import java.util.Optional;

public interface MaterialRepository {
    List<Material> findAll();
    Optional<Material> findById(int materialId);
    Material create(Material material);
    Material update(Material material);
    void archive(int materialId);
    String backendName();
}
