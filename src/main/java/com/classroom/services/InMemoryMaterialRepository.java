package com.classroom.services;

import com.spark.platform.models.Material;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryMaterialRepository implements MaterialRepository {

    private final Map<Integer, Material> store = new LinkedHashMap<>();
    private int nextId = 1;

    public InMemoryMaterialRepository() {
        seed();
    }

    @Override
    public List<Material> findAll() {
        List<Material> materials = new ArrayList<>();
        for (Material material : store.values()) {
            materials.add(copy(material));
        }
        materials.sort(Comparator.comparing(Material::getMaterialId));
        return materials;
    }

    @Override
    public Optional<Material> findById(int materialId) {
        Material material = store.get(materialId);
        return Optional.ofNullable(material == null ? null : copy(material));
    }

    @Override
    public Material create(Material material) {
        Material created = copy(material);
        created.setMaterialId(nextId++);
        if (created.getCreatedAt() == null) {
            created.setCreatedAt(Timestamp.from(Instant.now()));
        }
        store.put(created.getMaterialId(), copy(created));
        return copy(created);
    }

    @Override
    public Material update(Material material) {
        if (!store.containsKey(material.getMaterialId())) {
            throw new IllegalArgumentException("Material not found: " + material.getMaterialId());
        }

        Material updated = copy(material);
        Material existing = store.get(material.getMaterialId());
        if (updated.getCreatedAt() == null) {
            updated.setCreatedAt(existing.getCreatedAt());
        }

        store.put(updated.getMaterialId(), copy(updated));
        return copy(updated);
    }

    @Override
    public void archive(int materialId) {
        Material existing = store.get(materialId);
        if (existing == null) {
            throw new IllegalArgumentException("Material not found: " + materialId);
        }
        existing.setStatus("ARCHIVED");
        store.put(materialId, copy(existing));
    }

    @Override
    public String backendName() {
        return "In-memory";
    }

    private Material copy(Material source) {
        Material target = new Material();
        target.setMaterialId(source.getMaterialId());
        target.setCourseId(source.getCourseId());
        target.setUploadedBy(source.getUploadedBy());
        target.setParentId(source.getParentId());
        target.setType(source.getType());
        target.setTitle(source.getTitle());
        target.setFilePath(source.getFilePath());
        target.setContent(source.getContent());
        target.setPageCount(source.getPageCount());
        target.setQuestionCount(source.getQuestionCount());
        target.setCardCount(source.getCardCount());
        target.setTopic(source.getTopic());
        target.setVisibility(source.getVisibility());
        target.setStatus(source.getStatus());
        target.setCreatedAt(source.getCreatedAt());
        return target;
    }

    private void seed() {
        store.clear();

        create(seedMaterial("Java Avancé - Chapitre 1", "PDF", "/materials/java_ch1.pdf", "ACTIVE"));
        create(seedMaterial("Java Avancé - Chapitre 2", "PDF", "/materials/java_ch2.pdf", "ACTIVE"));
        create(seedMaterial("Introduction à l'IA", "PDF", "/materials/ia_intro.pdf", "ACTIVE"));
    }

    private Material seedMaterial(String title, String type, String filePath, String status) {
        Material material = new Material();
        material.setTitle(title);
        material.setType(type);
        material.setFilePath(filePath);
        material.setVisibility("PUBLIC");
        material.setStatus(status);
        material.setCreatedAt(Timestamp.from(Instant.now()));
        return material;
    }
}
