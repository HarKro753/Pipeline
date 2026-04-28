package com.pipeline.domain.repository;

public interface ProductAllergenRepository {
    void save(String productBarcode, String value);
}
