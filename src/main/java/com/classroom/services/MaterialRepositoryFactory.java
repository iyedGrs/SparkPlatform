package com.classroom.services;

import com.spark.platform.config.DatabaseConfig;

public final class MaterialRepositoryFactory {

    private MaterialRepositoryFactory() {
    }

    public static MaterialRepository createDefault() {
        try {
            JdbcMaterialRepository jdbcRepository = new JdbcMaterialRepository(DatabaseConfig.getInstance());
            if (jdbcRepository.isAvailable()) {
                return jdbcRepository;
            }
        } catch (RuntimeException ignored) {
        }

        return new InMemoryMaterialRepository();
    }
}
