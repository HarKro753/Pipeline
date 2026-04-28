package com.pipeline.domain.repository;

public interface ProductRelationRepository {
    void save(String productBarcode, String value);
}
