package com.pipeline.domain.repository;

public interface ProductStateRepository {
    void save(String productBarcode, String value);
}
