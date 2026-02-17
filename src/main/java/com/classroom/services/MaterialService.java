package com.classroom.services;

import com.spark.platform.models.Material;

import java.util.List;

public class MaterialService {

    private final MaterialRepository materialRepository;

    public MaterialService() {
        this(MaterialRepositoryFactory.createDefault());
    }

    public MaterialService(MaterialRepository materialRepository) {
        this.materialRepository = materialRepository;
    }

    public List<Material> listMaterials() {
        return materialRepository.findAll();
    }

    public Material saveMaterial(Material material) {
        validate(material);

        if (material.getVisibility() == null || material.getVisibility().isBlank()) {
            material.setVisibility("PUBLIC");
        }

        if (material.getStatus() == null || material.getStatus().isBlank()) {
            material.setStatus("ACTIVE");
        }

        if (material.getMaterialId() <= 0) {
            return materialRepository.create(material);
        }

        return materialRepository.update(material);
    }

    public void archiveMaterial(int materialId) {
        materialRepository.archive(materialId);
    }

    public String getStorageModeLabel() {
        return materialRepository.backendName();
    }

    private void validate(Material material) {
        if (material == null) {
            throw new IllegalArgumentException("Material is required");
        }

        String title = material.getTitle() == null ? "" : material.getTitle().trim();
        String type = material.getType() == null ? "" : material.getType().trim();
        String filePath = material.getFilePath() == null ? "" : material.getFilePath().trim();
        String content = material.getContent() == null ? "" : material.getContent().trim();

        if (title.isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }

        if (type.isBlank()) {
            throw new IllegalArgumentException("Type is required");
        }

        if ("PDF".equalsIgnoreCase(type) && filePath.isBlank()) {
            throw new IllegalArgumentException("PDF materials require an attached file");
        }

        if (filePath.isBlank() && content.isBlank()) {
            throw new IllegalArgumentException("Provide either an attachment or content");
        }

        material.setTitle(title);
        material.setType(type.toUpperCase());
        material.setFilePath(filePath.isBlank() ? null : filePath);
        material.setContent(content.isBlank() ? null : content);
    }
}
