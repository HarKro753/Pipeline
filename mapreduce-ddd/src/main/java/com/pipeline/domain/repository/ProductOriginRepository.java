package com.pipeline.domain.repository;

public interface ProductOriginRepository {
    void save(String productBarcode, String value);
}
