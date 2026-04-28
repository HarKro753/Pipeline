package com.pipeline.domain.repository;

public interface ProductNutrientLevelRepository {
    void save(String productBarcode, String value);
}
