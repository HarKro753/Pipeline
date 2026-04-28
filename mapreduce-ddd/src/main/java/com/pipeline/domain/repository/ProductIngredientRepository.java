package com.pipeline.domain.repository;

public interface ProductIngredientRepository {
    void save(String productBarcode, String value);
}
