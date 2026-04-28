package com.pipeline.domain.repository;

public interface ProductCategoryRepository {
    void save(String productBarcode, String value);
}
