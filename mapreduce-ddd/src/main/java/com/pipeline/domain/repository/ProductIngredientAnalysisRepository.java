package com.pipeline.domain.repository;

public interface ProductIngredientAnalysisRepository {
    void save(String productBarcode, String value);
}
